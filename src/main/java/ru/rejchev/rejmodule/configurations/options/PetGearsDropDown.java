package ru.rejchev.rejmodule.configurations.options;

import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.enums.PetGear;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PetGearsDropDown implements Dropdown.Options<PetGear> {


    @Override
    public Collection<PetGear> options() {
        return Arrays.stream(PetGear.values()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getText(PetGear value) {
        if (value == null) return "";
        return value.getName();
    }
}
