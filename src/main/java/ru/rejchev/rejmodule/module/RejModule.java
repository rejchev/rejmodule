package ru.rejchev.rejmodule.module;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.ModuleConfig;
import ru.rejchev.rejmodule.module.base.IModuleComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;
import ru.rejchev.rejmodule.module.base.IModuleProperty;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviour;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviourPost;
import ru.rejchev.rejmodule.module.base.actions.IActionBehaviourPre;
import ru.rejchev.rejmodule.module.base.actions.IActionLoad;
import ru.rejchev.rejmodule.module.components.*;
import ru.rejchev.rejmodule.module.components.antipush.CitadelAntiPushComponentPart;
import ru.rejchev.rejmodule.module.components.antipush.PetAntiPushComponentPart;

import java.util.*;
import java.util.stream.Stream;


@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RejModule implements Module {

    public static final int BasePriority = 0;

    @Getter(value = AccessLevel.PRIVATE)
    PluginAPI pluginAPI;

    @Getter(value = AccessLevel.PRIVATE)
    Collection<IModuleComponent> components;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PUBLIC)
    ModuleConfig config;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    boolean isInitialTick;

    @NonFinal
    @Setter(value = AccessLevel.PRIVATE)
    String status;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    boolean canWorking;

    @Getter(AccessLevel.PRIVATE)
    Map<String, IModuleProperty> properties;

    public static RejModule of(PluginAPI pluginAPI) {
        return new RejModule(pluginAPI);
    }

    public RejModule(PluginAPI pluginAPI) {
        this.isInitialTick = true;
        this.canWorking = true;
        this.pluginAPI = pluginAPI;
        this.status = null;
        this.properties = new HashMap<>();
        this.components = new LinkedList<>();
    }

    @Override
    public void onTickModule() {

        if(!isCanWorking())
            return;

        ModuleMonoContext ctx = (new ModuleMonoContext())
                .setBotApi(getPluginAPI())
                .setComponents(getComponents())
                .setPriority(BasePriority)
                .setProperties(getProperties());

        ModuleAction action;

        if(isInitialTick() && !(canWorking = initModule(ctx)))
            return;

        if(preTime(ctx).ordinal() == ModuleAction.Stop.ordinal())
            return;

        if((action = behaviourTime(ctx)).ordinal() == ModuleAction.Stop.ordinal())
            return;

        if(action.ordinal() < ModuleAction.Handled.ordinal())
            OutTime(ctx);
    }

    protected ModuleAction preTime(final IModuleContext ctx) {
        ModuleAction action = ModuleAction.Continue, buff;

        for (IActionBehaviourPre elem: streamComponents(IActionBehaviourPre.class).toList()) {

            buff = elem.preBehaviourAction(ctx);

            if(buff.ordinal() > action.ordinal())
                action = buff;
        }

        return action;
    }

    protected ModuleAction behaviourTime(final IModuleContext ctx) {

        ModuleAction action = ModuleAction.Continue, buff;
        for (IActionBehaviour elem: streamComponents(IActionBehaviour.class).toList()) {

            buff = elem.behaviourAction(ctx);

            if(buff.ordinal() > action.ordinal())
                action = buff;
        }

        return action;
    }

    protected void OutTime(final IModuleContext ctx) {
        streamComponents(IActionBehaviourPost.class).forEach(x -> x.postBehaviourAction(ctx));
    }

    public RejModule registerProperties(Map<String, IModuleProperty> values) {
        values.forEach(this::registerProperty);
        return this;
    }

    public <T extends IModuleProperty> Module registerProperty(String name, T value) {
        getProperties().put(name, value);
        return this;
    }

    protected boolean initModule(IModuleContext ctx) {
        isInitialTick = false;

        registerProperties(Map.of(
                "rejConfig", ModuleProperty.of(getConfig(), RejModule.BasePriority),
                "pluginAPI", ModuleProperty.of(getPluginAPI(), RejModule.BasePriority),
                "botAPI", ModuleProperty.of(getPluginAPI().requireAPI(BotAPI.class), RejModule.BasePriority),
                "petAPI", ModuleProperty.of(getPluginAPI().requireAPI(PetAPI.class), RejModule.BasePriority),
                "heroAPI", ModuleProperty.of(getPluginAPI().requireAPI(HeroAPI.class), RejModule.BasePriority),
                "configAPI", ModuleProperty.of(getPluginAPI().requireAPI(ConfigAPI.class), RejModule.BasePriority),
                "movementAPI", ModuleProperty.of(getPluginAPI().requireAPI(MovementAPI.class), RejModule.BasePriority),
                "entitiesAPI", ModuleProperty.of(getPluginAPI().requireAPI(EntitiesAPI.class), RejModule.BasePriority),
                "attackAPI", ModuleProperty.of(getPluginAPI().requireAPI(AttackAPI.class), RejModule.BasePriority),
                "starSystemAPI", ModuleProperty.of(getPluginAPI().requireAPI(StarSystemAPI.class), RejModule.BasePriority)
        ));

        registerProperties(Map.of(
                "heroItemsAPI", ModuleProperty.of(getPluginAPI().requireAPI(HeroItemsAPI.class), RejModule.BasePriority)
        ));

        registerProperties(Map.of(
                "mode", ModuleProperty.of(getConfig().getResearch_mode(), BasePriority),
                "pet", ModuleProperty.of(null, BasePriority),
                "ammo", ModuleProperty.of(1, BasePriority),
                "rocket", ModuleProperty.of(1, BasePriority),
                "rocketLauncher", ModuleProperty.of(1, BasePriority),
                "attack", ModuleProperty.of(null, BasePriority)
        ));

        getComponents().addAll(List.of(
                AntiPushComponent.of("antipush", 100,
                        new CitadelAntiPushComponentPart("citadel", Integer.MIN_VALUE),
                        new PetAntiPushComponentPart("pet", 100)),
                new PetComponent("pet", 1),
                new AttackLegacyComponent("attack", 1),
                new MovementLegacyComponent("movement", 1),
                new MapTravelComponent("travel", 1)
        ));

        for(IActionLoad elem: streamComponents(IActionLoad.class).toList()) {
            setStatus(elem.onLoad(ctx));

            if(getStatus() != null)
                break;
        }

        return getStatus() == null;
    }

    private <T> Stream<T> streamComponents(Class<T> clazz) {
        return getComponents().stream().map(clazz::cast).filter(Objects::nonNull);
    }

    @Override
    public void onTickStopped() {
        Module.super.onTickStopped();
    }

    @Override
    public boolean canRefresh() {
        return Module.super.canRefresh();
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getStoppedStatus() {
        return Module.super.getStoppedStatus();
    }

    public <T> T getProperty(String name, Class<T> clazz) {
        return getProperties().get(name).getValue(clazz);
    }
}
