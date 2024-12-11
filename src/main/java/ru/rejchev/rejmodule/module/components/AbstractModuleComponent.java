package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.PluginAPI;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.IModuleComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;

@SuperBuilder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractModuleComponent implements IModuleComponent {

    String signature;

    int priority;

    @NonFinal
    @Getter(AccessLevel.PROTECTED)
    PluginAPI pluginAPI;

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public ModuleAction behaviourAction(IModuleContext context) {
        return ModuleAction.Continue;
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {

    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        final IModuleProperty property = ctx.getProperty(getSignature());

        if(property != null && property.getPriority() > getPriority())
            return ModuleAction.Continue;

        ctx.getProperties().put(getSignature(), ModuleProperty.of(null, getPriority()));

        return ModuleAction.Change;
    }

    @Override
    public String onLoad(IModuleContext ctx) {

        if(getPluginAPI() == null)
            pluginAPI = ctx.getProperty("pluginAPI").getValue(PluginAPI.class);

        return null;
    }

    protected <T> T getProperty(IModuleContext ctx, String name, Class<T> clazz) {
        return ctx.getProperty(name).getValue(clazz);
    }
}
