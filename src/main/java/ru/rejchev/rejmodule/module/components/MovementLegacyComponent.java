package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.entities.Box;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.module.base.IModuleContext;

import java.util.Comparator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovementLegacyComponent extends AbstractModuleComponent {


    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    MovementAPI movementAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    HeroAPI heroAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    AttackAPI attackAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    StarSystemAPI starSystemAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    BotAPI botAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    EntitiesAPI entitiesAPI;

    @NonFinal
    boolean backwards;

    public MovementLegacyComponent(String signature, int priority) {
        super(signature, priority);
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {

        if(getProperty(context, "travel", GameMap.class) != null)
            return;

        if(getMovementAPI().isOutOfMap()) {
            getMovementAPI().moveRandom();
            return;
        }

        Box targetBox;
        if(context.getProperty("collect") != null
        && (targetBox = getProperty(context, "collect", Box.class)) != null) {

            if(getHeroAPI().distanceTo(targetBox) <= 250.0) {
                getMovementAPI().stop(false);
                return;
            }

            getMovementAPI().moveTo(targetBox);
            return;
        }

        Npc targetNpc;
        if((targetNpc = getProperty(context, "attack", Npc.class)) != null)
            moveToAnSafePosition(context, targetNpc);

        else if(getHeroAPI().distanceTo(getMovementAPI().getDestination()) < (double)20.0F)
            getMovementAPI().moveRandom();
    }

    protected boolean isTargetInPreferredRadius(Locatable target) {
        return (!getMovementAPI().isInPreferredZone(target) && getHeroAPI().distanceTo(target) <= 2000.0);
    }

    protected Locatable getConcurrentTarget(IModuleContext ctx) {

        final Npc targetNpc = getProperty(ctx, "attack", Npc.class);
        final Box targetBox = getProperty(ctx, "collect", Box.class);

        if(targetBox == null)
            return targetNpc;

        if(targetNpc == null)
            return targetBox;

        return getHeroAPI().distanceTo(targetBox) > getHeroAPI().distanceTo(targetNpc)
                ? targetNpc
                : targetBox;
    }

    protected void moveToAnSafePosition(IModuleContext ctx, Npc target) {
        Location targetLoc = target.getLocationInfo().destinationInTime(250);

        double distance = getHeroAPI().distanceTo(getAttackAPI().getTarget());
        double angle = targetLoc.angleTo(getHeroAPI());

        double radius = getAttackAPI().modifyRadius(599.0);

        if((target.getInfo()) != null)
            radius = getAttackAPI().modifyRadius(target.getInfo().getRadius());

        double speed = target.getSpeed();

        double angleDiff;
        {
            double maxRadFix = radius / 2,
                    radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
            distance = (radius += radiusFix);
            // Moved distance + speed - distance to chosen radius same angle, divided by radius
            angleDiff = Math.max((getHeroAPI().getSpeed() * 0.625) + (Math.max(200, speed) * 0.625)
                    - getHeroAPI().distanceTo(Location.of(targetLoc, angle, radius)), 0) / radius;
        }

        Location direction = getBestDir(targetLoc, angle, angleDiff, distance, target);
        searchValidLocation(direction, targetLoc, angle, distance);

        getMovementAPI().moveTo(direction);
    }

    protected Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance, Npc target) {
        int maxCircleIterations = 4;
        int iteration = 1;
        double forwardScore = 0;
        double backScore = 0;
        do {
            forwardScore += score(Locatable.of(targetLoc, angle + (angleDiff * iteration), distance), target);
            backScore += score(Locatable.of(targetLoc, angle - (angleDiff * iteration), distance), target);
            // Toggle direction if either one of the directions is perfect, or one is 300 better.
            if (forwardScore < 0 != backScore < 0 || Math.abs(forwardScore - backScore) > 300) break;
        } while (iteration++ < maxCircleIterations);

        if (iteration <= maxCircleIterations) backwards = backScore > forwardScore;
        return Location.of(targetLoc, angle + angleDiff * (backwards ? -1 : 1), distance);
    }

    protected void searchValidLocation(Location direction, Location targetLoc, double angle, double distance) {
        // Search in a spiral around the wanted position
        // MAX_LOCATION_SEARCH = 10_000
        while (!getMovementAPI().canMove(direction) && distance < 10_000) {
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        }

        // Found no valid location within in a spiral, settle for whatever
        // 10_000 = MAX_LOCATION_SEARCH
        if (distance >= 10_000)
            direction.toAngle(targetLoc, angle, 500);
    }

    protected double score(Locatable loc, Npc target) {
        return (getMovementAPI().canMove(loc) ? 0 : -1000) - getEntitiesAPI().getNpcs().stream() // Consider barrier as bad as 1000 radius units.
                .filter(n -> target != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }

    @Override
    public String onLoad(IModuleContext ctx) {
        if(getAttackAPI() == null)
            attackAPI = ctx.getProperty("attackAPI").getValue(AttackAPI.class);

        if(getHeroAPI() == null)
            heroAPI = ctx.getProperty("heroAPI").getValue(HeroAPI.class);

        if(getStarSystemAPI() == null)
            starSystemAPI = ctx.getProperty("starSystemAPI").getValue(StarSystemAPI.class);

        if(getBotAPI() == null)
            botAPI = ctx.getProperty("botAPI").getValue(BotAPI.class);

        if((entitiesAPI = getProperty(ctx, "entitiesAPI", EntitiesAPI.class)) == null)
            return "EntitiesAPI is required";

        if((movementAPI = getProperty(ctx, "movementAPI", MovementAPI.class)) == null)
            return "MovementAPI is required";

        return null;
    }
}
