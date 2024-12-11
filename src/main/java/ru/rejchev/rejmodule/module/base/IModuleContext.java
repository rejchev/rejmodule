package ru.rejchev.rejmodule.module.base;

import eu.darkbot.api.API;
import eu.darkbot.api.PluginAPI;
import ru.rejchev.rejmodule.module.ModuleAction;

import java.util.concurrent.TimeUnit;

public interface IModuleContext {

    PluginAPI api();

    IModuleProperty property(String name);

    ModuleAction property(String name, Object value, int priority);

    <C extends IModuleComponent> IModuleComponent component(Class<C> key);

    <C extends IModuleComponent> ModuleAction component(Class<C> key, IModuleComponent value);

    default <T extends IModuleProperty> T property(String name, Class<T> clazz) {
        try { return clazz.cast(property(name)); }
        catch (ClassCastException ignored) {}
        return null;
    }

    default <T extends API> T api(Class<T> clazz) {
        return api().requireAPI(clazz);
    }

    default <T extends IModuleComponent> IModuleProperty property(Class<T> componentClazz) {

        IModuleComponent component;
        if((component = component(componentClazz)) == null)
            return null;

        return property(component.getSignature());
    }

    default <T extends IModuleComponent> ModuleAction property(Class<T> componentClazz, Object value, int priority) {
        IModuleComponent component;
        if((component = component(componentClazz)) == null)
            return ModuleAction.Continue;

        return property(component.getSignature(), value, priority);
    }

    default long seconds() {
        return TimeUnit.MICROSECONDS.toSeconds(System.currentTimeMillis());
    }
}
