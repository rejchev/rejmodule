package ru.rejchev.rejmodule.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.module.base.IModuleProperty;

import java.util.Objects;


@FieldDefaults(level = AccessLevel.PRIVATE)
public final class ModuleProperty implements IModuleProperty {

    public static final int PriorityMask = 0x7FFF_FFFF;

    Object value;

    long priority;

    public static ModuleProperty of(Object value, int priority) {
        return new ModuleProperty(value, priority);
    }

    public ModuleProperty(Object value, int priority) {
        this.priority = (priority & PriorityMask);
        this.value = value;
    }

    @Override
    public int priority() {
        return (int) priority;
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public ModuleAction update(Object value, int priority) {
        if(priority() > (priority & PriorityMask))
            return ModuleAction.Continue;

        this.priority = (priority & PriorityMask);
        this.value = value;
        return ModuleAction.Change;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ModuleProperty that)) return false;
        return priority == that.priority && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.hashCode(), priority);
    }

    @Override
    public int compareTo(IModuleProperty o) {
        return Integer.compare(priority(), o.priority());
    }
}
