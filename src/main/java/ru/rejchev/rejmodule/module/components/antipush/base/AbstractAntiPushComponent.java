package ru.rejchev.rejmodule.module.components.antipush.base;

import ru.rejchev.rejmodule.module.base.AbstractComponent;

public abstract class AbstractAntiPushComponent extends AbstractComponent implements IAntiPushComponent {
    public AbstractAntiPushComponent(String signature, int priority) {
        super(signature, priority);
    }
}
