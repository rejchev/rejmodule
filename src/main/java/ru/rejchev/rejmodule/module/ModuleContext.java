package ru.rejchev.rejmodule.module;

import eu.darkbot.api.PluginAPI;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.module.base.IModuleComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class ModuleContext implements IModuleContext {

    PluginAPI pluginAPI;

    @Getter(AccessLevel.PRIVATE)
    final Map<String, IModuleProperty> properties = new HashMap<>();

    @Getter(AccessLevel.PRIVATE)
    final Map<Class<? extends IModuleComponent>, IModuleComponent> components = new HashMap<>();

    public static ModuleContext of(PluginAPI pluginAPI) {
        return new ModuleContext(pluginAPI);
    }

    public ModuleContext(PluginAPI pluginAPI) {
        this.pluginAPI = pluginAPI;
    }

    public ModuleContext api(PluginAPI value) {
        pluginAPI = value;
        return this;
    }

    public ModuleContext components(Map<Class<? extends IModuleComponent>, IModuleComponent> values) {
        components.putAll(values);
        return this;
    }

    public ModuleContext properties(Map<String, IModuleProperty> values) {
        properties.putAll(values);
        return this;
    }

    @Override
    public PluginAPI api() {
        return pluginAPI;
    }

    @Override
    public IModuleProperty property(String name) {
        return getProperties().get(name);
    }

    @Override
    public ModuleAction property(String name, Object value, int priority) {
        IModuleProperty property;

        if((property = getProperties().get(name)) == null)
            getProperties().put(name, (property = ModuleProperty.of(null, priority)));

        return property.update(value, priority);
    }

    @Override
    public <C extends IModuleComponent> C component(Class<C> clazz) {
        try { return clazz.cast(getComponents().get(clazz)); }
        catch (ClassCastException ignore) {}
        return null;
    }

    @Override
    public <C extends IModuleComponent> ModuleAction component(Class<C> key, IModuleComponent value) {

        final ModuleAction action = (getComponents().containsKey(key))
                ? ModuleAction.Change
                : ModuleAction.Continue;

        getComponents().put(key, value);
        return action;
    }

}
