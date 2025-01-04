package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.configurations.CoreModuleConfig;
import ru.rejchev.rejmodule.configurations.components.AttackComponentConfig;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.components.attack.NpcWrapper;

import java.util.Comparator;
import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BaseAttackComponent extends AbstractComponent {

    public static final String Signature = "attack";

    public static final int BasePriority = 1;

    private static BaseAttackComponent instance;

    public static BaseAttackComponent instance() {

        BaseAttackComponent p;

        if((p = instance) == null)
            instance = p = new BaseAttackComponent();

        return instance;
    }

    @Getter(value = AccessLevel.PRIVATE)
    AttackAPI attackAPI;

    @Getter(value = AccessLevel.PRIVATE)
    HeroAPI heroAPI;

    @Getter(value = AccessLevel.PRIVATE)
    EntitiesAPI entitiesAPI;

    @Getter(value = AccessLevel.PRIVATE)
    GroupAPI groupAPI;

    @Getter(value = AccessLevel.PRIVATE)
    MovementAPI movementAPI;

    @Getter(AccessLevel.PRIVATE)
    ConfigSetting<Boolean> onlyKillInPreferredZone;

    @Getter(AccessLevel.PRIVATE)
    AttackComponentConfig config;

    boolean backwards;

    @Getter(AccessLevel.PRIVATE)
    Locatable lastPreferredZoneLocation;

    private BaseAttackComponent() {
        super(Signature, BasePriority);
    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        super.preBehaviourAction(ctx);

        if(getLastPreferredZoneLocation() == null || getMovementAPI().isInPreferredZone(getHeroAPI()))
            lastPreferredZoneLocation = getHeroAPI().getLocationInfo().getCurrent();

        updateNpcBlackList();

        final NpcWrapper potentialTarget = getPotentialTarget(null);

        if (!getAttackAPI().hasTarget()
        || getAttackAPI().isBugged()
        || isFakeTargetInAttackRadius(getAttackAPI().getTarget()))
            ctx.property(getSignature(), (potentialTarget != null ? potentialTarget.getNpc() : null), getPriority());


        Optional.ofNullable(NpcWrapper.of(
                getProperty(ctx, getSignature(), Npc.class),
                this::getTargetPriority,
                this::isAttackedByOthers)).ifPresent(x -> {
            if (x.getNpc().getInfo() == null)
                return;

            NpcWrapper buf;

            if ((buf = getPotentialTarget(x)) == null)
                return;

            if (getAttackAPI().isAttacking())
                getAttackAPI().stopAttack();

            ctx.property(getSignature(), buf, getPriority());
        });

        return ModuleAction.Change;
    }

    @Override
    public ModuleAction behaviourAction(IModuleContext context) {

        Npc target;
        if((target = getProperty(context, getSignature(), Npc.class)) == null /*|| !getAttackAPI().hasTarget()*/)
            return ModuleAction.Continue;

        // TODO: user priority
        return context.property(BaseMovementComponent.class, getMovementLocation(target), getPriority());
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {

        if(getProperty(context, BaseMapTravelComponent.class, GameMap.class) != null) {
            if(getAttackAPI().hasTarget())
                getAttackAPI().getTargetAs(Npc.class).setBlacklisted(20_000);

            getAttackAPI().stopAttack();
            return;
        }

        final Lockable buff = getProperty(context, getSignature(), Npc.class);

//        if(buff != null)
//            System.out.println("buff.getId() = " + buff.getId() + "; " + buff.getEntityInfo().getUsername());

//        if(getAttackAPI().getTarget() != null && getAttackAPI().getTarget() != buff) {
//            if(getAttackAPI().isAttacking())
//                getAttackAPI().stopAttack();
//        }

        getAttackAPI().setTarget(buff);
//
//        if(isFakeTarget(buff))
//            return;

//        System.out.println("isTargetInAttackRadius(buff) = " + isTargetInAttackRadius(buff));

        if(getAttackAPI().hasTarget() /*&& isTargetInAttackRadius(buff)*/)
            getAttackAPI().tryLockAndAttack();
    }

    @Override
    public String onLoad(final IModuleContext ctx) {

        ModuleProperty moduleConfigProperty;
        if((moduleConfigProperty = ctx.property("config", ModuleProperty.class)) == null)
            return "Core module config is required";

        if((config = moduleConfigProperty.value(CoreModuleConfig.class).getAttack()) == null)
            return "Attack module config is required";

        heroAPI = ctx.api(HeroAPI.class);
        groupAPI = ctx.api(GroupAPI.class);
        attackAPI = ctx.api(AttackAPI.class);
        entitiesAPI = ctx.api(EntitiesAPI.class);
        movementAPI = ctx.api(MovementAPI.class);

//        final ConfigAPI configAPI = ctx.api(ConfigAPI.class);
        onlyKillInPreferredZone = ctx.api(ConfigAPI.class)
                .requireConfig("general.roaming.only_kill_preferred");

        return null;
    }

    private boolean isTargetInAttackRadius(Lockable target) {
        // ...
        return target != null && target.distanceTo(getHeroAPI()) <= 600;
    }

    private boolean isFakeTargetInAttackRadius(Lockable target) {
        return isFakeTarget(target) && isTargetInAttackRadius(target);
    }

    private boolean isFakeTarget(Lockable target) {
        // getAttackAPI().getTarget().getEntityInfo().getUsername() length is 0 ;/
        return !target.trySelect(false) && target.getId() == Integer.MIN_VALUE;
    }

    private NpcWrapper getPotentialTarget(NpcWrapper currentTarget) {

        return getEntitiesAPI().getNpcs().stream()
                .filter(x -> x != null && x.getInfo() != null && !x.isBlacklisted() && x.getInfo().getShouldKill())
                .map(x -> NpcWrapper.of(x, getTargetPriority(x), isAttackedByOthers(x)))
                .sorted(Comparator.comparingDouble(NpcWrapper::getPriority))
                .filter(x -> !getOnlyKillInPreferredZone().getValue() || isInPreferred(x.getNpc()))
                .filter(x -> x.getNpc().getInfo().hasExtraFlag(NpcFlag.IGNORE_ATTACKED) || !isAttackedByOthers(x.getNpc()))
                .filter(x -> !x.getNpc().getInfo().hasExtraFlag(NpcFlag.ATTACK_SECOND) || isAttackedByOthers(x.getNpc()))
                .filter(x -> !x.getNpc().getInfo().hasExtraFlag(NpcFlag.PASSIVE) || x.getNpc().isAttacking(getHeroAPI()))
                .filter(x -> isInvalidTarget(currentTarget) || x.getPriority() < currentTarget.getPriority())
                .findFirst()
                .orElse(null);
    }

    private boolean isInvalidTarget(NpcWrapper target) {
        return target == null || !target.isValid();
    }

    private boolean isInPreferred(Npc target) {

        if(getMovementAPI().isInPreferredZone(target))
            return true;

        return !getMovementAPI().isInPreferredZone(target) && getMovementAPI()
                .getDistanceBetween(target, getLastPreferredZoneLocation()) <= getConfig().getPreferred_radius();
    }

    private void updateNpcBlackList() {
        getEntitiesAPI().getNpcs().stream()
                .filter(x -> x != null && x.getInfo() != null)
                .forEach(x -> {
                    if(!x.getInfo().hasExtraFlag(NpcFlag.IGNORE_ATTACKED) && isAttackedByOthers(x))
                        x.setBlacklisted(20_000);
                });
    }

    private boolean isAttackedByOthers(Npc npc) {
        return getEntitiesAPI().getShips().stream()
                .anyMatch(x -> x.isAttacking(npc) && getGroupAPI().getMember(x) == null);
    }

    private double getHPBasedPriority(double k, Npc target) {
        return (-1 * k * (100 - ((int)(target.getHealth().hpPercent() * 100))));
    }

    private double getDistanceBasedPriority(double k, Npc target) {
//        return (-1 * k * (target.distanceTo(getHeroAPI())));
        return (-1 * k * (600/target.distanceTo(getHeroAPI())));
    }

    private double getPetLocatorBasedPriority(double k, Npc target) {
        return (-1 * k * 100 * (target.getInfo().hasExtraFlag(NpcFlag.PET_LOCATOR) ? 1 : 0));
    }

    // TODO: ...
    private double getTargetPriority(Npc target) {
        int sum;
        if((sum = sumPriorities()) == 0)
            sum = 1;

        return (getConfig().getPriorities().getUser() * target.getInfo().getPriority()
                + getPetLocatorBasedPriority(getConfig().getPriorities().getPet(), target)
                + getHPBasedPriority(getConfig().getPriorities().getHp(), target)
                + getDistanceBasedPriority(getConfig().getPriorities().getDistance(), target))
                / (sum * 100);
    }

    private int sumPriorities() {
        return getConfig().getPriorities().getUser()
            +  getConfig().getPriorities().getDistance()
            +  getConfig().getPriorities().getHp()
            +  getConfig().getPriorities().getPet();
    }


    private Location getMovementLocation(Npc target) {
        Location targetLoc = target.getLocationInfo().destinationInTime(250);

        double distance = getHeroAPI().distanceTo(target);
        double angle = targetLoc.angleTo(getHeroAPI());

        double radius = 560.0;

        if(target.getInfo() != null)
            radius = modifyRadius(target, target.getInfo().getRadius());

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

        return direction;
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

    private double modifyRadius(Npc target, double radius) {
        if (target.getHealth().hpPercent() < 0.25 && target.getInfo().hasExtraFlag(NpcFlag.AGGRESSIVE_FOLLOW))
            radius *= 0.75;

        if (!getHeroAPI().isAttacking() && target.isMoving() && angleDiff(target.getLocationInfo().getAngle(), getHeroAPI().angleTo(target)) > 1.6)
            radius = Math.min(560, radius);

        if (getAttackAPI().isCastingAbility())
            radius = Math.min(560, radius);

        if (!target.isMoving() || target.getHealth().hpPercent() < 0.25)
            radius = Math.min(600, radius);

        return radius;
    }

    private static double angleDiff(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % (Math.PI * 2);
        return phi > Math.PI ? ((Math.PI * 2) - phi) : phi;
    }
}
