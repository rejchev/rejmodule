package ru.rejchev.rejmodule;


import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.configurations.RejModuleConfig;
import ru.rejchev.rejmodule.module.ModuleCore;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Feature(name = "RejModule", description = "")
public final class RejModule implements Module, Configurable<RejModuleConfig> {

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    RejModuleConfig config;

    @Getter(AccessLevel.PRIVATE)
    ModuleCore module;

    public RejModule(PluginAPI pluginAPI) {
        this.module = ModuleCore.of(pluginAPI);
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
    public void setConfig(ConfigSetting<RejModuleConfig> configSetting) {
        getModule().setConfig((this.config = configSetting.getValue()).getModule());
    }
}
