package ru.rejchev.rejmodule.module.components.antipush;

import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.other.EntityInfo;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.configurations.components.CitadelAntiPushComponentConfig;
import ru.rejchev.rejmodule.configurations.CoreModuleConfig;
import ru.rejchev.rejmodule.configurations.details.AntiPushActionReason;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.components.AntiPushComponent;
import ru.rejchev.rejmodule.module.components.BaseAttackComponent;
import ru.rejchev.rejmodule.module.components.antipush.base.AbstractAntiPushComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

@Getter(AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class CitadelDrawFireAntiPush extends AbstractAntiPushComponent {

    public static final String Signature = AntiPushComponent.Signature + ".citadel";

    public static final int BasePriority = 100;

    private static CitadelDrawFireAntiPush instance;

    public static CitadelDrawFireAntiPush instance() {

        CitadelDrawFireAntiPush c;

        if((c = instance) == null)
            instance = c = new CitadelDrawFireAntiPush();

        return instance;
    }

    long nextSaveSeconds;

    final int citadelDrawFireVictim = 39;

    @Setter(AccessLevel.PRIVATE)
    CitadelAntiPushComponentConfig config;

    final Collection<Ship> pushers;

    Collection<? extends Ship> mapShips;

    HeroAPI heroAPI;

    AttackAPI attackAPI;

    HeroItemsAPI heroItemsAPI;

    private CitadelDrawFireAntiPush() {
        super(Signature, BasePriority);
        pushers = new HashSet<>();
    }

    @Override
    public String onLoad(final IModuleContext ctx) {

        ModuleProperty moduleConfigProperty;
        if((moduleConfigProperty = ctx.property("config", ModuleProperty.class)) == null)
            return "Core module config is required";

        if((config = moduleConfigProperty.value(CoreModuleConfig.class).getAntipush().getCitadel()) == null)
            return "Antipush citadel module config is required";

        heroAPI = ctx.api(HeroAPI.class);
        attackAPI = ctx.api(AttackAPI.class);
        heroItemsAPI = ctx.api(HeroItemsAPI.class);
        mapShips = ctx.api(EntitiesAPI.class).getShips();

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

        if(getNextSaveSeconds() > context.seconds())
            return ModuleAction.Continue;

        return context.property(BaseAttackComponent.class, null, getPriority());
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {

        Ship[] pushers;
        if((pushers = getProperty(context, getSignature(), Ship[].class)) == null)
            return;

        if(getConfig().getAbility() != null)
            getHeroItemsAPI().useItem(getConfig().getAbility());

        Arrays.stream(pushers).forEach(x -> getPushers().add(x));
    }

    @Override
    public ModuleAction preBehaviourAction(final IModuleContext ctx) {
        super.preBehaviourAction(ctx);

        final long seconds = ctx.seconds();

        final Ship[] potentialShips = getPotentialShips(getHeroAPI(), getConfig().getRadius());

        if(isShipHasPotentialPushingEffect(getHeroAPI()) || potentialShips.length > 0)
            nextSaveSeconds = seconds + getAntiPushEffectActiveDuration();

        return seconds < getNextSaveSeconds()
                ? ctx.property(getSignature(), potentialShips, getPriority())
                : ModuleAction.Continue;
    }
}
