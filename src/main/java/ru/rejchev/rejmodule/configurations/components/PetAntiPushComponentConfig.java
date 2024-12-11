package ru.rejchev.rejmodule.configurations.components;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.game.enums.PetGear;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.configurations.details.AntiPushActionReason;
import ru.rejchev.rejmodule.configurations.options.AntiPushReasonDropDown;
import ru.rejchev.rejmodule.configurations.options.PetGearsDropDown;

import java.util.HashSet;
import java.util.Set;

@Getter
@FieldDefaults(level = AccessLevel.PUBLIC)
public class PetAntiPushComponentConfig {

    @Number(min = -1, max = 4_000_000, step = 10)
    int timeout = 1800;

    @Number(min = -1, max = 4_000_000, step = 1000)
    int radius = -1;

    @Option
    @Dropdown(options = AntiPushReasonDropDown.class, multi = true)
    public Set<AntiPushActionReason> hide_reasons = new HashSet<>();

    @Number(min = 0, max = 1, step = 1)
    boolean use_repairer = false;

    @Option
    @Dropdown(options = PetGearsDropDown.class, multi = true)
    public Set<PetGear> overridable_gears = new HashSet<>();
}
