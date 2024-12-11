package ru.rejchev.rejmodule.module.base;

import eu.darkbot.api.API;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.ModuleProperty;

@SuperBuilder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractComponent implements IModuleComponent {

    String signature;

    int priority;

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public ModuleAction behaviourAction(IModuleContext context) {
        return ModuleAction.Continue;
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {}

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        return ctx.property(getSignature(), null, getPriority());
    }

    @Override
    public String onLoad(IModuleContext ctx) {
        return null;
    }

    protected <T> T getProperty(IModuleContext ctx, String name, Class<T> clazz) {

        IModuleProperty property;
        if((property = ctx.property(name)) == null)
            return null;

        return property.value(clazz);
    }

    protected <C extends IModuleComponent, R>
    R getProperty(IModuleContext ctx, Class<C> component, Class<R> clazz) {

        IModuleComponent buf;
        if((buf = ctx.component(component)) == null)
            return null;

        return ctx.property(buf.getSignature()).value(clazz);
    }
}
