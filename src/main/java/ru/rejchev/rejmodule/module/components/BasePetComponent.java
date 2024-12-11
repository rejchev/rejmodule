package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.game.entities.Pet;
import eu.darkbot.api.game.enums.PetGear;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.PetAPI;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.module.ModuleAction;
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;

import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class BasePetComponent extends AbstractComponent {

    public static final String Signature = "pet";

    public static final int Priority = 1;

    private static BasePetComponent instance;

    public static BasePetComponent instance() {

        BasePetComponent p;

        if((p = instance) == null)
            instance = p = new BasePetComponent();

        return instance;
    }

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

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    ConfigSetting<Boolean> petEnableSetting;

    @NonFinal
    @Getter(AccessLevel.PRIVATE)
    ConfigSetting<PetGear> petGearSetting;

    private BasePetComponent() {
        super(Signature, Priority);
    }

    @Override
    public ModuleAction preBehaviourAction(IModuleContext ctx) {
        ModuleAction action = super.preBehaviourAction(ctx);

        if(petEnableSetting.getValue())
            action = ctx.property(getSignature(), petGearSetting.getValue(), getPriority());

        return action;
    }

    @Override
    public void postBehaviourAction(final IModuleContext context) {
        if(getHeroAPI().getPet().isEmpty())
            return;

        PetGear gear = context.property(getSignature()).value(PetGear.class);

        if(getLastGear() == null)
            lastGear = gear;

        if(gear == null
        || !getPetAPI().hasGear(gear)
        || getProperty(context, BaseMapTravelComponent.class, GameMap.class) != null) {
            petSetEnabled(false, null);
            return;
        }

        final long seconds = getSeconds();

        if(getLastGear() != PetGear.REPAIR && gear == PetGear.REPAIR)
            nextGearCheck = seconds + 2;

        if(getLastGear() == PetGear.REPAIR && gear == PetGear.REPAIR && nextGearCheck <= seconds) {

            final Pet pet = getHeroAPI().getPet().orElse(null);

            if(pet != null && !pet.getHealth().hpDecreasedIn(100))
                gear = PetGear.PASSIVE;
        }

        petSetEnabled(true, gear);

        if(getLastGear() != gear)
            lastGear = gear;
    }

    @Override
    public String onLoad(IModuleContext  ctx) {
        petAPI = ctx.api().requireAPI(PetAPI.class);
        heroAPI = ctx.api().requireAPI(HeroAPI.class);

        petEnableSetting = ctx.api().requireAPI(ConfigAPI.class).requireConfig("pet.enabled");
        petGearSetting = ctx.api().requireAPI(ConfigAPI.class).requireConfig("pet.module_id");

        return null;
    }

    private void petSetEnabled(boolean value, PetGear petGear) {

        if(!value)
            petGear = null;

        if(petGearSetting.getValue() != petGear)
            petGearSetting.setValue(petGear);

        if(petEnableSetting.getValue() != value)
            petEnableSetting.setValue(value);

        getPetAPI().setEnabled(value);
    }

    public long getSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
