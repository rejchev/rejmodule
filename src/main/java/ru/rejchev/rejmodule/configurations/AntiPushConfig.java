package ru.rejchev.rejmodule.configurations;

import eu.darkbot.api.config.annotations.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PUBLIC)
public class AntiPushConfig {

    @Option
    AntiPushCitadelConfig citadel = new AntiPushCitadelConfig();

    @Option
    AntiPushPetConfig pet = new AntiPushPetConfig();
}
