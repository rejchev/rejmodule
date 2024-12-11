package ru.rejchev.rejmodule.configurations.options;

import eu.darkbot.api.config.annotations.Dropdown;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.rejchev.rejmodule.configurations.details.AntiPushActionReason;

import java.util.Arrays;
import java.util.Collection;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AntiPushReasonDropDown implements Dropdown.Options<AntiPushActionReason> {
    @Override
    public Collection<AntiPushActionReason> options() {
        return Arrays.stream(AntiPushActionReason.values()).toList();
    }

    @Override
    public String getText(AntiPushActionReason value) {
        if (value == null)
            return "";

        return value.getSpecialName();
    }
}
