package ru.rejchev.rejmodule.module.components;

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
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BaseMovementComponent extends AbstractComponent {

    public static final String Signature = "movement";

    public static final int BasePriority = 1;

    private static BaseMovementComponent instance;

    public static BaseMovementComponent instance() {

        BaseMovementComponent p;

        if((p = instance) == null)
            instance = p = new BaseMovementComponent();

        return instance;
    }

    @Getter(AccessLevel.PRIVATE)
    MovementAPI movementAPI;

    @Getter(AccessLevel.PRIVATE)
    HeroAPI heroAPI;

    @Getter(AccessLevel.PRIVATE)
    AttackAPI attackAPI;

    @Getter(AccessLevel.PRIVATE)
    StarSystemAPI starSystemAPI;

    @Getter(AccessLevel.PRIVATE)
    BotAPI botAPI;

    @Getter(AccessLevel.PRIVATE)
    EntitiesAPI entitiesAPI;

    boolean backwards;

    private BaseMovementComponent() {
        super(Signature, BasePriority);
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {

        if(getProperty(context, BaseMapTravelComponent.class, GameMap.class) != null)
            return;

        if(getMovementAPI().isOutOfMap()) {
            getMovementAPI().moveRandom();
            return;
        }

        Box targetBox;
        if((targetBox = getProperty(context, BaseCollectorComponent.class, Box.class)) != null) {

            if(getHeroAPI().distanceTo(targetBox) <= 250.0) {
                getMovementAPI().stop(false);
                return;
            }

            getMovementAPI().moveTo(targetBox);

            return;
        }

        Npc targetNpc;
        if((targetNpc = getProperty(context, BaseAttackComponent.class, Npc.class)) != null)
            moveToAnSafePosition(context, targetNpc);

        else if(getHeroAPI().distanceTo(getMovementAPI().getDestination()) < (double)20.0F)
            getMovementAPI().moveRandom();
    }

    private void moveToAnSafePosition(IModuleContext ctx, Npc target) {
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

    private Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance, Npc target) {
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

    private void searchValidLocation(Location direction, Location targetLoc, double angle, double distance) {
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

    private double score(Locatable loc, Npc target) {
        return (getMovementAPI().canMove(loc) ? 0 : -1000) - getEntitiesAPI().getNpcs().stream() // Consider barrier as bad as 1000 radius units.
                .filter(n -> target != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }

    @Override
    public String onLoad(IModuleContext ctx) {

        botAPI = ctx.api(BotAPI.class);
        heroAPI = ctx.api(HeroAPI.class);
        attackAPI = ctx.api(AttackAPI.class);
        entitiesAPI = ctx.api(EntitiesAPI.class);
        movementAPI = ctx.api(MovementAPI.class);
        starSystemAPI = ctx.api(StarSystemAPI.class);

        return null;
    }
}
