package ru.rejchev.rejmodule.module.components;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.components.antipush.CitadelDrawFireAntiPush;
import ru.rejchev.rejmodule.module.components.antipush.PetAntiPush;
import ru.rejchev.rejmodule.module.components.antipush.base.IAntiPushComponent;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AntiPushComponent extends AbstractComponent {

    public static final String Signature = "antipush";

    public static final int BasePriority = 100;

    private static AntiPushComponent instance;

    public static AntiPushComponent instance() {

        AntiPushComponent c;

        if((c = instance) == null)
            instance = c = new AntiPushComponent();

        return instance;
    }

    @Getter(value = AccessLevel.PRIVATE)
    List<IAntiPushComponent> components;

    private AntiPushComponent() {
        super(Signature, BasePriority);
        components = new LinkedList<>();
    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        return initAction(ctx);
    }

    @Override
    public ModuleAction behaviourAction(IModuleContext ctx) {
        return initAction(ctx);
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {
        getComponents().forEach(x -> x.postBehaviourAction(context));
    }

    @Override
    public String onLoad(IModuleContext ctx) {

        register(List.of(PetAntiPush.instance(), CitadelDrawFireAntiPush.instance()));

        String status;
        for(IAntiPushComponent component : getComponents()) {
            if(component == null)
                continue;

            if((status = component.onLoad(ctx)) != null)
                return status;
        }

        return null;
    }

    private ModuleAction initAction(IModuleContext ctx) {
        ModuleAction action = ModuleAction.Continue, buf;

        for(IAntiPushComponent component : getComponents()) {
            if(component != null && (buf = component.preBehaviourAction(ctx)).ordinal() > action.ordinal()) {
                if(buf.ordinal() >= ModuleAction.Handled.ordinal())
                    buf = ModuleAction.Change;

                action = buf;
            }
        }

        return action;
    }

    public <T extends IAntiPushComponent> AntiPushComponent register(T value) {
        getComponents().add(value);
        return this;
    }

    public <T extends IAntiPushComponent> AntiPushComponent register(List<T> antiPushComponents) {
        antiPushComponents.forEach(this::register);
        return this;
    }
}
