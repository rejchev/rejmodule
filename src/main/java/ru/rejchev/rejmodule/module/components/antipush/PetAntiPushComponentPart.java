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
import ru.rejchev.rejmodule.configurations.AntiPushPetConfig;
import ru.rejchev.rejmodule.configurations.ModuleConfig;
import ru.rejchev.rejmodule.configurations.details.AntiPushActionReason;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;
import ru.rejchev.rejmodule.module.components.antipush.base.IAntiPushComponent;

import java.util.*;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PetAntiPushComponentPart implements IAntiPushComponent {
    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    long nextSaveSeconds;

    @Getter
    int citadelDrawFireVictim = 39;

    @Getter
    @Setter
    @NonFinal
    AntiPushPetConfig config;

    @Getter
    Collection<Ship> pushers = new HashSet<>();

    @Getter
    @NonFinal
    Collection<? extends Ship> mapShips = null;

    @Getter
    @NonFinal
    HeroAPI heroAPI = null;

    @Getter
    @NonFinal
    AttackAPI attackAPI = null;

    @Getter
    @NonFinal
    PetAPI petAPI = null;

    @Getter
    String signature;

    long priority;

    @Getter
    @NonFinal
    long approachTimeOutTimePoint;

    public PetAntiPushComponentPart(String signature, int priority) {

        assert (priority > 0);
        assert (signature != null && !signature.isEmpty());

        this.signature = signature;
        this.priority = priority;
    }

    @Override
    public long getAntiPushEffectActiveDuration() {
        return getConfig().getTimeout();
    }

    @Override
    public ModuleAction behaviourAction(final IModuleContext context) {

        ModuleProperty property;
        if((property = (ModuleProperty) context.getProperty("pet")) == null)
            return ModuleAction.Continue;

        if(property.getPriority() > getPriority())
            return ModuleAction.Continue;

        PetGear gear = null;
        if(!context.getProperties().containsKey(getSignature()) && !context.getProperties().containsKey(getSignature() + "Ex")) {

            if(!getConfig().isUse_repairer())
                return ModuleAction.Continue;

            if(!getConfig().getOverridable_gears().contains(property.getValue(PetGear.class)))
                return ModuleAction.Continue;

            if(getPetAPI().getHealth().getHp() >= (getPetAPI().getHealth().getMaxHp() - 1000))
                return ModuleAction.Continue;

            gear = PetGear.REPAIR;
        }

        property.setValue(gear, getPriority());

        return ModuleAction.Change;
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {
        final IModuleProperty property = context.getProperty(getSignature());

        if(property != null && property.getValue(Ship[].class) != null)
            Arrays.stream(property.getValue(Ship[].class)).forEach(x -> getPushers().add(x));


        context.getProperties().remove(getSignature());
        context.getProperties().remove(getSignature() + "Ex");
    }

    @Override
    public ModuleAction preBehaviourAction(final IModuleContext ctx) {
        final long seconds = getSeconds();

        if(getMapShips().stream().anyMatch(x -> isInRadius(x, getHeroAPI(), getConfig().getRadius()) && isBadGuy(x)))
            approachTimeOutTimePoint = seconds + 15L;

        if(getPetAPI().isEnabled()) getHeroAPI().getPet().ifPresent(pet -> {
            if(pet.getHealth().hpDecreasedIn(100)) {

                final Ship[] potentialPushers = getMapShips().stream().filter(x -> isAttacking(x, pet)).toArray(Ship[]::new);

                if(potentialPushers.length == 0)
                    return;

                nextSaveSeconds = seconds + getAntiPushEffectActiveDuration();

                ctx.getProperties().put(getSignature(), ModuleProperty.of(potentialPushers, getPriority()));
            }
        });

        if(getApproachTimeOutTimePoint() > seconds)
            ctx.getProperties().put("petAntiPushEx", ModuleProperty.of(true, getPriority()));

        return getApproachTimeOutTimePoint() > seconds || getNextSaveSeconds() > seconds
                ? ModuleAction.Change
                : ModuleAction.Continue;
    }

    @Override
    public String onLoad(final IModuleContext ctx) {
        if(getAttackAPI() == null)
            attackAPI = ctx.getProperty("attackAPI").getValue(AttackAPI.class);

        if(getHeroAPI() == null)
            heroAPI = ctx.getProperty("heroAPI").getValue(HeroAPI.class);

        if(getMapShips() == null)
            mapShips = ctx.getProperty("entitiesAPI").getValue(EntitiesAPI.class).getShips();

        if(getPetAPI() == null)
            petAPI = ctx.getProperty("petAPI").getValue(PetAPI.class);

        ModuleConfig moduleConfig;
        if((moduleConfig = ctx.getProperty("rejConfig").getValue(ModuleConfig.class)) == null)
            return "RejModule config is required";

        if(moduleConfig.getAntipush() != null)
            config = moduleConfig.getAntipush().getPet();

        return null;
    }

    private boolean isAttacking(final Ship ship, final Pet pet) {
        return ship.getTarget() == pet || ship.isAttacking(pet) || ship.isAiming(pet);
    }

    private long getSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
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

    public int getPriority() {
        return (int) priority;
    }
}
