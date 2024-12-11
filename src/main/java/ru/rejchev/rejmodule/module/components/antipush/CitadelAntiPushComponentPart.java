package ru.rejchev.rejmodule.module.components.antipush;

import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.other.EntityInfo;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.AntiPushCitadelConfig;
import ru.rejchev.rejmodule.configurations.ModuleConfig;
import ru.rejchev.rejmodule.configurations.details.AntiPushActionReason;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.components.antipush.base.IAntiPushComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CitadelAntiPushComponentPart implements IAntiPushComponent {

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    long nextSaveSeconds;

    @Getter
    int citadelDrawFireVictim = 39;

    @Getter
    @Setter
    @NonFinal
    AntiPushCitadelConfig config;

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
    HeroItemsAPI heroItemsAPI = null;

    @Getter
    String signature;

    long priority;

    public CitadelAntiPushComponentPart(String signature, int priority) {
        assert signature != null;
        assert !signature.isEmpty();

        this.signature = signature;
        this.priority = priority;
    }

    @Override
    public String onLoad(final IModuleContext ctx) {
        if(attackAPI == null)
            attackAPI = ctx.getProperty("attackAPI").getValue(AttackAPI.class);

        if(heroAPI == null)
            heroAPI = ctx.getProperty("heroAPI").getValue(HeroAPI.class);

        if(getMapShips() == null)
            mapShips = ctx.getProperty("entitiesAPI").getValue(EntitiesAPI.class).getShips();

        if(getHeroItemsAPI() == null)
            heroItemsAPI = ctx.getProperty("heroItemsAPI").getValue(HeroItemsAPI.class);

        ModuleConfig moduleConfig;
        if((moduleConfig = ctx.getProperty("rejConfig").getValue(ModuleConfig.class)) == null)
            return "RejModule config is required";

        if(moduleConfig.getAntipush() != null)
            config = moduleConfig.getAntipush().getCitadel();

        return null;
    }

    private Ship[] getPotentialShips(Ship ship, double radius) {

        return getMapShips().stream()
                .filter(x -> radius < 0 || isInRadius(x, ship, radius))
                .filter(this::isBadGuy)
                .toArray(Ship[]::new);
    }

    private boolean isShipHasPotentialPushingEffect(Ship ship) {
        return ship.hasEffect(EntityEffect.DRAW_FIRE) || ship.hasEffect(getCitadelDrawFireVictim());
    }

    private long getSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

    private boolean isBadGuy(Ship ship) {
        return isShipHasPotentialPushingEffect(ship) || isPusher(ship) || isEnemy(ship) || isLowDiplomancy(ship);
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
        return a.distanceTo(b) < radius;
    }

    private boolean isReasonUsed(AntiPushActionReason reason) {
        return getConfig().getAttack_stop_reasons().contains(reason);
    }

    @Override
    public long getAntiPushEffectActiveDuration() {
        return getConfig().getDuration();
    }

    @Override
    public ModuleAction behaviourAction(final IModuleContext context) {

        if(getNextSaveSeconds() > getSeconds())
            return ModuleAction.Continue;

        ModuleProperty property;
        if((property = (ModuleProperty) context.getProperty("attack")) == null)
            return ModuleAction.Continue;

        if(property.getPriority() > getPriority())
            return ModuleAction.Continue;

        property.setValue(null, getPriority());

        return ModuleAction.Change;
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {

        if(!context.getProperties().containsKey(getSignature()))
            return;

        if(getConfig().getAbility() != null)
            getHeroItemsAPI().useItem(getConfig().getAbility());

        Arrays.stream(context.getProperty(getSignature()).getValue(Ship[].class)).forEach(x -> getPushers().add(x));
    }

    @Override
    public ModuleAction preBehaviourAction(final IModuleContext ctx) {
        final Ship[] potentialShips = getPotentialShips(getHeroAPI(), getConfig().getRadius());

        if(isShipHasPotentialPushingEffect(getHeroAPI()) || potentialShips.length > 0)
            nextSaveSeconds = getSeconds() + getAntiPushEffectActiveDuration();

        final long seconds = getSeconds();

        if(seconds < getNextSaveSeconds())
            ctx.getProperties().put(getSignature(), ModuleProperty.of(potentialShips, 100));

        return seconds < getNextSaveSeconds() ? ModuleAction.Change : ModuleAction.Continue;
    }

    public int getPriority() {
        return (int) priority;
    }

}
