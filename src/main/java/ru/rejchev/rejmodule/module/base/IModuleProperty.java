package ru.rejchev.rejmodule.module.base;

public interface IModuleProperty extends Comparable<IModuleProperty> {

    int getPriority();

    Object getValue();

    default <T> T getValue(Class<T> clazz) {
        try {
            return clazz.cast(getValue());
        } catch (ClassCastException ignored){}
        return null;
    }
}
