package ru.rejchev.rejmodule.module.components.antipush;

import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.enums.PetGear;
import eu.darkbot.api.game.other.EntityInfo;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.components.PetAntiPushComponentConfig;
import ru.rejchev.rejmodule.configurations.CoreModuleConfig;
import ru.rejchev.rejmodule.configurations.details.AntiPushActionReason;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;
import ru.rejchev.rejmodule.module.components.AntiPushComponent;
import ru.rejchev.rejmodule.module.components.BasePetComponent;
import ru.rejchev.rejmodule.module.components.antipush.base.AbstractAntiPushComponent;

import java.util.*;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PetAntiPush extends AbstractAntiPushComponent {

    public static final String Signature = AntiPushComponent.Signature + ".pet";

    public static final String ApproachSignature = Signature + ".approach";

    public static final int BasePriority = 100;

    private static PetAntiPush instance;

    public static PetAntiPush instance() {

        PetAntiPush c;

        if((c = instance) == null)
            instance = c = new PetAntiPush();

        return instance;
    }

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    long nextSaveSeconds;

    @Getter
    int citadelDrawFireVictim = 39;

    @Getter
    @Setter
    @NonFinal
    PetAntiPushComponentConfig config;

    @Getter
    Collection<Ship> pushers;

    @Getter
    @NonFinal
    Collection<? extends Ship> mapShips;

    @Getter
    @NonFinal
    HeroAPI heroAPI;

    @Getter
    @NonFinal
    AttackAPI attackAPI;

    @Getter
    @NonFinal
    PetAPI petAPI;

    @Getter
    @NonFinal
    long approachTimeOutTimePoint;

    private PetAntiPush() {
        super(Signature, BasePriority);
        pushers = new HashSet<>();
    }

    @Override
    public long getAntiPushEffectActiveDuration() {
        return getConfig().getTimeout();
    }

    @Override
    public ModuleAction behaviourAction(final IModuleContext context) {

        IModuleProperty petGearProperty;
        if((petGearProperty = context.property(BasePetComponent.class)) == null)
            return ModuleAction.Continue;

        if(petGearProperty.priority() > getPriority())
            return ModuleAction.Continue;

        PetGear gear = null;
        if(getProperty(context, getSignature(), Ship[].class) == null
        && getProperty(context, getApproachSignature(), Boolean.class) == null) {

            if(!getConfig().isUse_repairer())
                return ModuleAction.Continue;

            if(!getConfig().getOverridable_gears().contains(petGearProperty.value(PetGear.class)))
                return ModuleAction.Continue;

            if(getPetAPI().getHealth().getHp() >= (getPetAPI().getHealth().getMaxHp() - 1000))
                return ModuleAction.Continue;

            gear = PetGear.REPAIR;
        }

        return context.property(BasePetComponent.class, gear, getPriority());
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {
        final IModuleProperty property = context.property(getSignature());

        if(property != null && property.value(Ship[].class) != null)
            Arrays.stream(property.value(Ship[].class)).forEach(x -> getPushers().add(x));
    }

    @Override
    public ModuleAction preBehaviourAction(final IModuleContext ctx) {
        ctx.property(getSignature(), null, getPriority());
        ctx.property(getApproachSignature(), null, getPriority());

        final long seconds = ctx.seconds();

        if(getMapShips().stream().anyMatch(x -> isInRadius(x, getHeroAPI(), getConfig().getRadius()) && isBadGuy(x)))
            approachTimeOutTimePoint = seconds + 15L;

        if(getPetAPI().isEnabled()) getHeroAPI().getPet().ifPresent(pet -> {
            if(pet.getHealth().hpDecreasedIn(100)) {

                final Ship[] potentialPushers = getMapShips().stream()
                        .filter(x -> isAttacking(x, pet)).toArray(Ship[]::new);

                if(potentialPushers.length == 0)
                    return;

                nextSaveSeconds = seconds + getAntiPushEffectActiveDuration();

                ctx.property(getSignature(), potentialPushers, getPriority());
            }
        });

        if(getApproachTimeOutTimePoint() > seconds)
            ctx.property(getApproachSignature(), true, getPriority());

        return getApproachTimeOutTimePoint() > seconds || getNextSaveSeconds() > seconds
                ? ModuleAction.Change
                : ModuleAction.Continue;
    }

    @Override
    public String onLoad(final IModuleContext ctx) {

        ModuleProperty moduleConfigProperty;
        if((moduleConfigProperty = ctx.property("config", ModuleProperty.class)) == null)
            return "Core module config is required";

        if((config = moduleConfigProperty.value(CoreModuleConfig.class).getAntipush().getPet()) == null)
            return "Antipush pet module config is required";

        petAPI = ctx.api(PetAPI.class);
        heroAPI = ctx.api(HeroAPI.class);
        attackAPI = ctx.api(AttackAPI.class);
        mapShips = ctx.api(EntitiesAPI.class).getShips();

        return null;
    }

    private boolean isAttacking(final Ship ship, final Pet pet) {
        return ship.getTarget() == pet || ship.isAttacking(pet) || ship.isAiming(pet);
    }

    private boolean isBadGuy(Ship ship) {
        return isPusher(ship) || isEnemy(ship) || isLowDiplomancy(ship);
    }

    private boolean isPusher(Ship ship) {
        return isReasonUsed(AntiPushActionReason.PusherInRadius)
                && getPushers().contains(ship);
    }

    private boolean isEnemy(Ship ship) {
        return isReasonUsed(AntiPushActionReason.EnemyInRadius)
                && ship.getEntityInfo().isEnemy();

    }

    private boolean isLowDiplomancy(Ship ship) {
        return isReasonUsed(AntiPushActionReason.LowDiplomancyInRadius)
                && ship.getEntityInfo().getClanDiplomacy().ordinal() > EntityInfo.Diplomacy.ALLIED.ordinal();
    }

    private boolean isInRadius(Ship a, Ship b, double radius) {
        return radius < 0 || a.distanceTo(b) < radius;
    }

    private boolean isReasonUsed(AntiPushActionReason reason) {
        return getConfig().getHide_reasons().contains(reason);
    }

    private String getApproachSignature() {
        return PetAntiPush.ApproachSignature;
    }
}
