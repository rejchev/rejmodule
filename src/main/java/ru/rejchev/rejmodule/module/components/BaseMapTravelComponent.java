package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.shared.modules.MapModule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class BaseMapTravelComponent extends AbstractComponent {

    public static final String Signature = "travel";

    public static final int BasePriority = 1;

    private static BaseMapTravelComponent instance;

    public static BaseMapTravelComponent instance() {

        BaseMapTravelComponent p;

        if((p = instance) == null)
            instance = p = new BaseMapTravelComponent();

        return instance;
    }

    @NonFinal
    @Getter(AccessLevel.PROTECTED)
    BotAPI botAPI;

    @NonFinal
    @Getter(AccessLevel.PROTECTED)
    StarSystemAPI starSystemAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    ConfigAPI configAPI;

    private BaseMapTravelComponent() {
        super(Signature, BasePriority);
    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        ModuleAction action = super.preBehaviourAction(ctx);

        GameMap workingMap;
        if((workingMap = getWorkingMap(getConfigAPI(), getStarSystemAPI())) == null)
            return action;

        if(workingMap == getStarSystemAPI().getCurrentMap())
            return action;

        return ctx.property(getSignature(), workingMap, getPriority());
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {

        final GameMap travelMap = context.property(getSignature()).value(GameMap.class);

        if(travelMap == null || travelMap == getStarSystemAPI().getCurrentMap())
            return;

        getBotAPI().setModule(context.api().requireInstance(MapModule.class)).setTarget(travelMap);
    }

    @Override
    public String onLoad(IModuleContext ctx) {

        botAPI = ctx.api(BotAPI.class);
        configAPI = ctx.api(ConfigAPI.class);
        starSystemAPI = ctx.api(StarSystemAPI.class);

        return null;
    }

    private static GameMap getWorkingMap(ConfigAPI configAPI, StarSystemAPI starSystemAPI) {
        return starSystemAPI.getOrCreateMap(((Integer)configAPI.requireConfig("general.working_map").getValue()));
    }
}
