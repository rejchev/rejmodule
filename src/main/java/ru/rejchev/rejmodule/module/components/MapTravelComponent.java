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
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MapTravelComponent extends AbstractModuleComponent {

    @NonFinal
    @Getter(AccessLevel.PROTECTED)
    BotAPI botAPI;

    @NonFinal
    @Getter(AccessLevel.PROTECTED)
    StarSystemAPI starSystemAPI;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    ConfigAPI configAPI;

    public MapTravelComponent(String signature, int priority) {
        super(signature, priority);
    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        IModuleProperty moduleProperty;

        if((moduleProperty = ctx.getProperty(getSignature())) != null && moduleProperty.getPriority() > getPriority())
            return ModuleAction.Continue;

        final GameMap workingMap = getWorkingMap(getConfigAPI(), getStarSystemAPI());

        ctx.getProperties().put(getSignature(), new ModuleProperty(
                (workingMap != null && workingMap != getStarSystemAPI().getCurrentMap()) ? workingMap : null,
                getPriority())
        );

        return ModuleAction.Change;
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {

        final GameMap travelMap = context.getProperty(getSignature()).getValue(GameMap.class);

        if(travelMap == null || travelMap == getStarSystemAPI().getCurrentMap())
            return;

        getBotAPI().setModule(getPluginAPI().requireInstance(MapModule.class)).setTarget(travelMap);
    }

    @Override
    public String onLoad(IModuleContext ctx) {
        String err = null;

        if((err = super.onLoad(ctx)) != null)
            return err;

        if((botAPI = getProperty(ctx, "botAPI", BotAPI.class)) == null)
            err = "BotAPI is required";

        if((starSystemAPI = getProperty(ctx, "starSystemAPI", StarSystemAPI.class)) == null)
            err = "StarSystemAPI is required";

        if((configAPI = getProperty(ctx, "configAPI", ConfigAPI.class)) == null)
            err = "StarSystemAPI is required";

        return null;
    }

    private static GameMap getWorkingMap(ConfigAPI configAPI, StarSystemAPI starSystemAPI) {
        return starSystemAPI.getOrCreateMap(((Integer)configAPI.requireConfig("general.working_map").getValue()));
    }
}
