package ru.rejchev.rejmodule.module.base;


import ru.rejchev.rejmodule.module.base.actions.IActionBehaviour;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviourPost;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviourPre;
import ru.rejchev.rejmodule.module.base.actions.IActionLoad;

public interface IModuleComponent extends
        IActionLoad, IActionBehaviour, IActionBehaviourPost, IActionBehaviourPre {

    default <T extends IModuleComponent> T cast(Class<T> clazz) {
        try { return clazz.cast(this); }
        catch (ClassCastException ignored) {}
        return null;
    }

    int getPriority();

    String getSignature();
}
