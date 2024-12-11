package ru.rejchev.rejmodule.configurations.details;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AntiPushActionReason {
    PusherInRadius ("PusherInRadius"),
    EnemyInRadius ("EnemyInRadius"),
    LowDiplomancyInRadius("LowDiplomancyInRadius");

    public AntiPushActionReason of(String specialName) {
        return Arrays.stream(values()).filter(x -> x.name().equals(specialName))
                .findFirst().orElse(null);
    }

    private final String specialName;

    AntiPushActionReason(String specialName) {
        this.specialName = specialName;
    }
}
