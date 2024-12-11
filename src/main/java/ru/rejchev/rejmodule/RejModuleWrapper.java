package ru.rejchev.rejmodule;


import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.RejModuleConfiguration;
import ru.rejchev.rejmodule.module.RejModule;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Feature(name = "RejModule", description = "Extended kill & collect module")
public class RejModuleWrapper implements Module, Configurable<RejModuleConfiguration> {

    @Getter(value = AccessLevel.PRIVATE)
    EventBrokerAPI eventBrokerAPI;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    RejModuleConfiguration config;

    @Getter(AccessLevel.PRIVATE)
    RejModule module;

    @Getter(AccessLevel.PRIVATE)
    PluginAPI pluginAPI;

    public RejModuleWrapper(PluginAPI pluginAPI, EventBrokerAPI eventBrokerAPI) {
        this.pluginAPI = pluginAPI;
        this.eventBrokerAPI = eventBrokerAPI;
        this.module = RejModule.of(pluginAPI);
    }

    @Override
    public void onTickModule() {
        getModule().onTickModule();
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
        return Module.super.getStatus();
    }

    @Override
    public String getStoppedStatus() {
        return Module.super.getStoppedStatus();
    }

    @Override
    public void setConfig(ConfigSetting<RejModuleConfiguration> configSetting) {
        this.config = configSetting.getValue();
        getModule().setConfig(getConfig().getModule());
    }
}
