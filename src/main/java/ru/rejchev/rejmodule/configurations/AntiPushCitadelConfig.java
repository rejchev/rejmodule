package ru.rejchev.rejmodule.configurations;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.game.items.SelectableItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.configurations.options.AbilityDropDown;
import ru.rejchev.rejmodule.configurations.options.AntiPushReasonDropDown;

import java.util.HashSet;
import java.util.Set;

@Getter
@FieldDefaults(level = AccessLevel.PUBLIC)
public class AntiPushCitadelConfig {

    @Number(min = -1, max = 4_000, step = 50)
    int radius = 2000;

    @Number(min = -1, max = 4_000_000, step = 10)
    int duration = 10;

    @Option
    @Dropdown(options = AntiPushReasonDropDown.class, multi = true)
    public Set<AbilityDropDown> attack_stop_reasons = new HashSet<>();

    @Option
    @Dropdown(options = AbilityDropDown.class)
    public SelectableItem.Ability ability = SelectableItem.Ability.CITADEL_DRAW_FIRE;
}
