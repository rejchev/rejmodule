package ru.rejchev.rejmodule.configurations.options;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.items.SelectableItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FormationDropDown implements Dropdown.Options<SelectableItem.Formation>{

    @Override
    public Collection<SelectableItem.Formation> options() {
        return Arrays.stream(SelectableItem.Formation.values()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getText(SelectableItem.Formation value) {
        if (value == null)
            return "";

        return value.name();
    }
}
