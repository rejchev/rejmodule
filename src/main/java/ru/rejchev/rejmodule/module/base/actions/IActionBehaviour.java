package ru.rejchev.rejmodule.module.base.actions;

import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.base.IModuleAction;
import ru.rejchev.rejmodule.module.base.IModuleContext;

public interface IActionBehaviour extends IModuleAction {
    ModuleAction behaviourAction(final IModuleContext context);
}
