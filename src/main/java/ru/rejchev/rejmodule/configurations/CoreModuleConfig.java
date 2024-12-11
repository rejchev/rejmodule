package ru.rejchev.rejmodule.configurations;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.game.items.SelectableItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.configurations.components.AntiPushComponentConfig;
import ru.rejchev.rejmodule.configurations.components.AttackComponentConfig;
import ru.rejchev.rejmodule.configurations.options.FormationDropDown;

// TODO: ...

@Getter
@FieldDefaults(level = AccessLevel.PUBLIC)
public class CoreModuleConfig {

    @Option
    AttackComponentConfig attack = new AttackComponentConfig();

    @Option
    public ShipModeConfiguration research_mode = new ShipModeConfiguration();

    @Getter
    public static class ShipModeConfiguration {

        @Number(min = 1, max = 2, step = 1)
        public int configuration = 1;

        @Option
        @Dropdown(options = FormationDropDown.class)
        public SelectableItem.Formation formation = SelectableItem.Formation.STANDARD;
    }

    @Option
    public AntiPushComponentConfig antipush = new AntiPushComponentConfig();
}
