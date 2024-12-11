package ru.rejchev.rejmodule.configurations;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Configuration(value = "rejmodule")
@FieldDefaults(level = AccessLevel.PUBLIC)
public class RejModuleConfig {

    @Option
    CoreModuleConfig module = new CoreModuleConfig();
}
