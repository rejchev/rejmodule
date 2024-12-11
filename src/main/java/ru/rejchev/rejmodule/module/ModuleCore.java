package ru.rejchev.rejmodule.module;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Module;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.CoreModuleConfig;
import ru.rejchev.rejmodule.module.base.IModuleAction;
import ru.rejchev.rejmodule.module.base.IModuleComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviour;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviourPost;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviourPre;
import ru.rejchev.rejmodule.module.base.actions.IActionLoad;
import ru.rejchev.rejmodule.module.components.*;

import java.util.*;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ModuleCore implements Module {

    public static final int BasePriority = 0;

    @Getter(value = AccessLevel.PRIVATE)
    PluginAPI pluginAPI;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PUBLIC)
    CoreModuleConfig config;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    boolean isInitialTick;

    @NonFinal
    @Setter(value = AccessLevel.PRIVATE)
    String status;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    boolean canWorking;

    @Getter(AccessLevel.PRIVATE)
    Map<String, IModuleProperty> properties;

    @Getter(value = AccessLevel.PRIVATE)
    Map<Class<? extends IModuleComponent>, IModuleComponent> components;

    public static ModuleCore of(PluginAPI pluginAPI) {
        return new ModuleCore(pluginAPI);
    }

    public ModuleCore(PluginAPI pluginAPI) {
        this.isInitialTick = true;
        this.canWorking = true;
        this.pluginAPI = pluginAPI;
        this.status = null;
        this.properties = new HashMap<>();
        this.components = new HashMap<>();
    }

    @Override
    public void onTickModule() {

        if(!isCanWorking())
            return;

        final ModuleContext ctx = ModuleContext.of(getPluginAPI())
                .components(getComponents())
                .properties(getProperties());

        ModuleAction action;

        if(isInitialTick() && !(canWorking = initModule(ctx)))
            return;

        if(preTime(ctx).ordinal() == ModuleAction.Stop.ordinal())
            return;

        if((action = behaviourTime(ctx)).ordinal() == ModuleAction.Stop.ordinal())
            return;

        if(action.ordinal() < ModuleAction.Handled.ordinal())
            outTime(ctx);
    }

    private ModuleAction preTime(final IModuleContext ctx) {
        ModuleAction action = ModuleAction.Continue, buff;

        for (IActionBehaviourPre elem : streamActions(IActionBehaviourPre.class).toList()) {

            buff = elem.preBehaviourAction(ctx);

            if(buff.ordinal() > action.ordinal())
                action = buff;
        }

        return action;
    }

    private ModuleAction behaviourTime(final IModuleContext ctx) {

        ModuleAction action = ModuleAction.Continue, buff;
        for (IActionBehaviour elem : streamActions(IActionBehaviour.class).toList()) {

            buff = elem.behaviourAction(ctx);

            if(buff.ordinal() > action.ordinal())
                action = buff;
        }

        return action;
    }

    private void outTime(final IModuleContext ctx) {
        streamActions(IActionBehaviourPost.class).forEach(x -> x.postBehaviourAction(ctx));
    }

    public ModuleCore properties(Map<String, IModuleProperty> values) {
        values.forEach(this::register);
        return this;
    }

    public <T extends IModuleProperty> ModuleCore register(String name, T value) {
        getProperties().put(name, value);
        return this;
    }

    public <T extends IModuleComponent> ModuleCore register(Class<T> key, IModuleComponent value) {
        getComponents().put(key, value);
        return this;
    }

    public <T extends IModuleComponent> ModuleCore components(Map<Class<T>, IModuleComponent> values) {
        values.forEach(this::register);
        return this;
    }

    private boolean initModule(IModuleContext ctx) {
        isInitialTick = false;

        register("config", ModuleProperty.of(getConfig(), ModuleCore.BasePriority));

        // base components
        register(BasePetComponent.class, BasePetComponent.instance());
        register(BaseAttackComponent.class, BaseAttackComponent.instance());
        register(BaseMovementComponent.class, BaseMovementComponent.instance());
        register(BaseMapTravelComponent.class, BaseMapTravelComponent.instance());

        // extended components
        register(AntiPushComponent.class, AntiPushComponent.instance());

        for(IActionLoad elem : streamActions(IActionLoad.class).toList()) {
            setStatus(elem.onLoad(ctx));

            if(getStatus() != null)
                break;
        }

        return getStatus() == null;
    }

    private <T extends IModuleAction> Stream<T> streamActions(Class<T> clazz) {
        return getComponents().values().stream().map(clazz::cast).filter(Objects::nonNull);
    }

    @Override
    public void onTickStopped() {
        Module.super.onTickStopped();
    }

    @Override
    public boolean canRefresh() {
        return Module.super.canRefresh();
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getStoppedStatus() {
        return Module.super.getStoppedStatus();
    }

    public <T> T getProperty(String name, Class<T> clazz) {
        return getProperties().get(name).value(clazz);
    }
}
