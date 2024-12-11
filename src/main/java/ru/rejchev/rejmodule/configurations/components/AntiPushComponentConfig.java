package ru.rejchev.rejmodule.configurations.components;

import eu.darkbot.api.config.annotations.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PUBLIC)
public class AntiPushComponentConfig {

    @Option
    CitadelAntiPushComponentConfig citadel = new CitadelAntiPushComponentConfig();

    @Option
    PetAntiPushComponentConfig pet = new PetAntiPushComponentConfig();
}
