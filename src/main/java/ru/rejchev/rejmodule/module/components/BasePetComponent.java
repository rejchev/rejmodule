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
import ru.rejchev.rejmodule.module.ModuleProperty;
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;

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

        PetGear gear;

        getPetEnableSetting().setValue(((gear = getProperty(context, getSignature(), PetGear.class)) != null));

        final long seconds = context.seconds();

        if(getLastGear() != PetGear.REPAIR && gear == PetGear.REPAIR)
            nextGearCheck = seconds + 5;

        if(getLastGear() == PetGear.REPAIR && gear == PetGear.REPAIR && nextGearCheck <= seconds) {

            final Pet pet = getHeroAPI().getPet().orElse(null);

            if(pet != null && !pet.getHealth().hpIncreasedIn(100))
                gear = PetGear.PASSIVE;
        }

        if(getLastGear() != (gear = updatePet(context, gear)))
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

    private PetGear updatePet(IModuleContext context, PetGear petGear) {

        if(petGear != null && getProperty(context, BaseMapTravelComponent.class, GameMap.class) != null)
            petGear = null;

        if(petGear != null && !getPetAPI().hasGear(petGear))
            petGear = PetGear.PASSIVE;

        if(petGear != null && petGearSetting.getValue() != petGear)
            petGearSetting.setValue(petGear);

        getPetAPI().setEnabled(petGear != null);

        return petGear;
    }
}
