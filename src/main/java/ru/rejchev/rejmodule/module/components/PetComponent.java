package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.enums.PetGear;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.utils.ItemNotEquippedException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.module.base.IModuleContext;

import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PetComponent extends AbstractModuleComponent {

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    PetAPI petAPI = null;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    HeroAPI heroAPI = null;

    @NonFinal
    @Getter(value = AccessLevel.PRIVATE)
    PetGear lastGear = null;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    long nextGearCheck = 0;

    public PetComponent(String signature, int priority) {
        super(signature, priority);
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {
        if(getHeroAPI().getPet().isEmpty())
            return;

        PetGear gear = context.getProperty(getSignature()).getValue(PetGear.class);

        if(getLastGear() == null)
            lastGear = gear;

        if(gear == null || !getPetAPI().hasGear(gear) || getProperty(context, "travel", GameMap.class) != null) {
            getPetAPI().setEnabled(false);
            return;
        }

        getPetAPI().setEnabled(true);

        final long seconds = getSeconds();

        if(getLastGear() != PetGear.REPAIR && gear == PetGear.REPAIR)
            nextGearCheck = seconds + 2;

        if(getLastGear() == PetGear.REPAIR && gear == PetGear.REPAIR && nextGearCheck <= seconds) {

            final Pet pet = getHeroAPI().getPet().orElse(null);

            if(pet != null && !pet.getHealth().hpDecreasedIn(100))
                gear = PetGear.PASSIVE;
        }

        try { getPetAPI().setGear(gear); }
        catch (ItemNotEquippedException ignored) {}

        if(getLastGear() != gear)
            lastGear = gear;
    }

    @Override
    public String onLoad(IModuleContext  ctx) {
        String err = null;

        if((err = super.onLoad(ctx)) != null)
            return err;

        if((petAPI = getProperty(ctx, "petAPI", PetAPI.class)) == null)
            err = "PetAPI is required";

        if((heroAPI = getProperty(ctx, "heroAPI", HeroAPI.class)) == null)
            err = "HeroAPI is required";

        return null;
    }

    public long getSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
