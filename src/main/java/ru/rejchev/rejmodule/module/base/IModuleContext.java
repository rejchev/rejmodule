package ru.rejchev.rejmodule.module.base;

import eu.darkbot.api.PluginAPI;

import java.util.*;

public interface IModuleContext {

    PluginAPI getApi();

    int getPriority();

    <T extends IModuleComponent> Collection<T> getComponents();

    default <T extends IModuleComponent> Collection<T> getComponents(Class<T> clazz) {
        return getComponents().stream().map(x -> {
            try { return clazz.cast(x); }
            catch (ClassCastException ignored) {}
            return null;
        }).filter(Objects::nonNull).toList();
    }

    Map<String, IModuleProperty> getProperties();

    default IModuleProperty getProperty(String name) {
        return getProperties().get(name);
    }

    default <T extends IModuleProperty> T getProperty(String name, Class<T> clazz) {
        try { return clazz.cast(getProperties().get(name)); }
        catch (ClassCastException ignored) {}
        return null;
    }
}
