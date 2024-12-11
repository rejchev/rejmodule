package ru.rejchev.rejmodule.module.base.actions;

import ru.rejchev.rejmodule.module.base.IModuleAction;
import ru.rejchev.rejmodule.module.base.IModuleContext;

public interface IActionBehaviourPost extends IModuleAction {
    void postBehaviourAction(final IModuleContext context);
}
