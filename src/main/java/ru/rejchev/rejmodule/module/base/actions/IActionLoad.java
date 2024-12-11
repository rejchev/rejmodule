package ru.rejchev.rejmodule.module.base.actions;


import ru.rejchev.rejmodule.module.base.IModuleAction;
import ru.rejchev.rejmodule.module.base.IModuleContext;

public interface IActionLoad extends IModuleAction {
    String onLoad(final IModuleContext ctx);
}
