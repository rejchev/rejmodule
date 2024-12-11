package ru.rejchev.rejmodule.configurations.components;

import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PUBLIC)
public class AttackComponentConfig {

    @Option
    AttackPriorityRule priorities = new AttackPriorityRule();

    @Getter
    @FieldDefaults(level = AccessLevel.PUBLIC)
    public static class AttackPriorityRule {

        @Number(min = 0, max = 100, step = 1)
        int user = 40;

        @Number(min = 0, max = 100, step = 1)
        int pet = 30;

        @Number(min = 0, max = 100, step = 1)
        int hp = 20;

        @Number(min = 0, max = 100, step = 1)
        int distance = 10;
    }
}
