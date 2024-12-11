package ru.rejchev.rejmodule.module;

import eu.darkbot.api.PluginAPI;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.module.base.IModuleComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;

import java.util.Collection;
import java.util.Map;


@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class ModuleContext implements IModuleContext {

    PluginAPI botAPI;

    long priority;

    Map<String, IModuleProperty> properties;

    Collection<? extends IModuleComponent> components;

    public ModuleContext setBotApi(PluginAPI value) {
        botAPI = value;
        return this;
    }

    public ModuleContext setPriority(int value) {
        priority = value;
        return this;
    }

    public <T extends IModuleComponent> ModuleContext setComponents(Collection<T> value) {
        components = value;
        return this;
    }

    public ModuleContext setProperties(Map<String, IModuleProperty> value) {
        properties = value;
        return this;
    }

    @Override
    public PluginAPI getApi() {
        return botAPI;
    }

    @Override
    public int getPriority() {
        return (int) priority;
    }

    @Override
    public <T extends IModuleComponent> Collection<T> getComponents() {
        return (Collection<T>) components;
    }

    @Override
    public Map<String, IModuleProperty> getProperties() {
        return properties;
    }
}