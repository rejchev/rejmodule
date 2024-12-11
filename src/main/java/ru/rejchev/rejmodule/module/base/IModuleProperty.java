package ru.rejchev.rejmodule.module.base;

import ru.rejchev.rejmodule.module.ModuleAction;

public interface IModuleProperty extends Comparable<IModuleProperty> {

    int priority();

    Object value();

    ModuleAction update(Object value, int priority);

    default <T> T value(Class<T> clazz) {
        try { return clazz.cast(value());}
        catch (ClassCastException ignored){}
        return null;
    }
}
