package ru.rejchev.rejmodule.module.components;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.AntiPushConfig;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.base.IModuleComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.components.antipush.base.IAntiPushComponent;

import java.util.*;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AntiPushComponent implements IModuleComponent {

    long priority;

    @Getter
    String signature;

    @Getter(value = AccessLevel.PRIVATE)
    List<IAntiPushComponent> components;

    public static AntiPushComponent of(String signature, int priority, IAntiPushComponent... components) {
        return (new AntiPushComponent(signature, priority)).addAll((components != null) ? List.of(components) : List.of());
    }

    public AntiPushComponent(String signature, int priority) {
        this.priority = priority;
        this.signature = signature;
        this.components = new LinkedList<>();
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

    public AntiPushComponent addAll(List<IAntiPushComponent> antiPushComponents) {
        getComponents().addAll(antiPushComponents.stream().filter(Objects::nonNull).toList());
        return this;
    }

    public int getPriority() {
        return (int) priority;
    }
}
