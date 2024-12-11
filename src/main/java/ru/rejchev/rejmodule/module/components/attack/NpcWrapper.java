package ru.rejchev.rejmodule.module.components.attack;

import eu.darkbot.api.game.entities.Npc;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;
import java.util.function.Function;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class NpcWrapper {
    Npc npc;

    Double priority;

    boolean isAttackedByOthers;

    public static NpcWrapper of(Npc npc, Double priority, boolean isAttackedByOthers) {
        return new NpcWrapper(npc, priority, isAttackedByOthers);
    }

    public static NpcWrapper of(Npc npc, Function<Npc, Double> funcPriority, Function<Npc, Boolean> funcAttacked) {

        if(npc == null || npc.getInfo() == null)
            return null;

        return new NpcWrapper(npc, funcPriority.apply(npc), funcAttacked.apply(npc));
    }

    public boolean isValid() {
        return npc != null && npc.getInfo() != null && priority != null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NpcWrapper)) return false;
        NpcWrapper that = (NpcWrapper) object;
        return isAttackedByOthers == that.isAttackedByOthers && Objects.equals(npc, that.npc) && Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(npc, priority, isAttackedByOthers);
    }
}
