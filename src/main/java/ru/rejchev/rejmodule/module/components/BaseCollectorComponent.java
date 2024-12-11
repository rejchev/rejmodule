package ru.rejchev.rejmodule.module.components;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.module.base.AbstractComponent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BaseCollectorComponent extends AbstractComponent {

    public static final String Signature = "collect";

    public static final int BasePriority = 1;

    private static BaseCollectorComponent instance;

    public static BaseCollectorComponent instance() {

        BaseCollectorComponent c;
        if((c = instance) == null)
            instance = c = new BaseCollectorComponent();

        return instance;
    }

    private BaseCollectorComponent() {
        super(Signature, BasePriority);
    }


}
