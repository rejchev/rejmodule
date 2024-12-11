package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.ModuleConfig;
import ru.rejchev.rejmodule.configurations.components.AttackComponentConfiguration;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.components.attack.NpcWrapper;

import java.util.Comparator;
import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttackLegacyComponent extends AbstractModuleComponent {

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    AttackAPI attackAPI;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    HeroAPI heroAPI;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    EntitiesAPI entitiesAPI;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    GroupAPI groupAPI;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    MovementAPI movementAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    ConfigSetting<Boolean> onlyKillInPreferredZone;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    AttackComponentConfiguration config;

    public AttackLegacyComponent(String signature, int priority) {
        super(signature, priority);
    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        ModuleAction action = super.preBehaviourAction(ctx);

        updateNpcBlackList();

        final NpcWrapper potentialTarget = getPotentialTarget(null);

        if (!getAttackAPI().hasTarget()
        || getAttackAPI().isBugged()
        || isFakeTargetInAttackRadius(getAttackAPI().getTarget()))
            ctx.getProperties().put(getSignature(), ModuleProperty.of(
                    (potentialTarget != null ? potentialTarget.getNpc() : null),
                    getPriority())
            );


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

            ctx.getProperties().put(getSignature(), ModuleProperty.of(buf.getNpc(), getPriority()));
        });


        return ModuleAction.Change;
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {

        if(getProperty(context, "travel", GameMap.class) != null) {
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
        String err;

        if((err = super.onLoad(ctx)) != null)
            return err;

        ModuleConfig moduleConfig;
        if((moduleConfig = getProperty(ctx, "rejConfig", ModuleConfig.class)) == null)
            return "RejModule Config is required";

        if((config = moduleConfig.getAttack()) == null)
            return "Attack module config is required";

        if((heroAPI = getProperty(ctx, "heroAPI", HeroAPI.class)) == null)
            err = "HeroAPI is required";

        if((attackAPI = getProperty(ctx, "attackAPI", AttackAPI.class)) == null)
            err = "AttackAPI is required";

        if((entitiesAPI = getProperty(ctx, "entitiesAPI", EntitiesAPI.class)) == null)
            err = "EntitiesAPI is required";

        groupAPI = getPluginAPI().requireAPI(GroupAPI.class);
        movementAPI = getPluginAPI().requireAPI(MovementAPI.class);

        final ConfigAPI configAPI = getPluginAPI().requireAPI(ConfigAPI.class);

        onlyKillInPreferredZone = configAPI.requireConfig("general.roaming.only_kill_preferred");

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

    private boolean isInPreferred(Locatable target) {
        return getMovementAPI().isInPreferredZone(target) || (!getMovementAPI().isInPreferredZone(target) && getHeroAPI().distanceTo(target) <= 2000.0);
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
        final int sum = sumPriorities();

        return (getConfig().getPriorities().getUser() * target.getInfo().getPriority()
                + getPetLocatorBasedPriority(getConfig().getPriorities().getPet(), target)
                + getHPBasedPriority(getConfig().getPriorities().getHp(), target)
                + getDistanceBasedPriority(getConfig().getPriorities().getDistance(), target))
                / (((sum == 0) ? 1 : sum) * 100);
    }

    private int sumPriorities() {
        return getConfig().getPriorities().getUser()
            +  getConfig().getPriorities().getDistance()
            +  getConfig().getPriorities().getHp()
            +  getConfig().getPriorities().getPet();
    }
}
