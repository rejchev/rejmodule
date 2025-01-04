package ru.rejchev.rejmodule.module.components;

import eu.darkbot.api.game.entities.Box;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.managers.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ru.rejchev.rejmodule.module.base.AbstractComponent;
import ru.rejchev.rejmodule.module.base.IModuleContext;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BaseMovementComponent extends AbstractComponent {

    public static final String Signature = "movement";

    public static final int BasePriority = 1;

    private static BaseMovementComponent instance;

    public static BaseMovementComponent instance() {

        BaseMovementComponent p;

        if((p = instance) == null)
            instance = p = new BaseMovementComponent();

        return instance;
    }

    @Getter(AccessLevel.PRIVATE)
    MovementAPI movementAPI;

    @Getter(AccessLevel.PRIVATE)
    HeroAPI heroAPI;

    @Getter(AccessLevel.PRIVATE)
    AttackAPI attackAPI;

    @Getter(AccessLevel.PRIVATE)
    StarSystemAPI starSystemAPI;

    @Getter(AccessLevel.PRIVATE)
    BotAPI botAPI;

    @Getter(AccessLevel.PRIVATE)
    EntitiesAPI entitiesAPI;

    private BaseMovementComponent() {
        super(Signature, BasePriority);
    }

    @Override
    public void postBehaviourAction(IModuleContext context) {

        if(getProperty(context, BaseMapTravelComponent.class, GameMap.class) != null)
            return;

        if(getMovementAPI().isOutOfMap()) {
            getMovementAPI().moveRandom();
            return;
        }

//        Box targetBox;
//        if((targetBox = getProperty(context, BaseCollectorComponent.class, Box.class)) != null) {
//
//            if(getHeroAPI().distanceTo(targetBox) <= 250.0) {
//                getMovementAPI().stop(false);
//                return;
//            }
//
//            getMovementAPI().moveTo(targetBox);
//
//            return;
//        }

        Locatable location;
        if((location = getProperty(context, getSignature(), Locatable.class)) != null) {

            if(getHeroAPI().distanceTo(location) <= (20.0 + (230.0 * (((location instanceof Box)) ? 1 : 0)))) {
                getMovementAPI().stop(false);
                return;
            }

            getMovementAPI().moveTo(location);
        }

        else if(getHeroAPI().distanceTo(getMovementAPI().getDestination()) < 20.0)
            getMovementAPI().moveRandom();
    }


    @Override
    public String onLoad(IModuleContext ctx) {

        botAPI = ctx.api(BotAPI.class);
        heroAPI = ctx.api(HeroAPI.class);
        attackAPI = ctx.api(AttackAPI.class);
        entitiesAPI = ctx.api(EntitiesAPI.class);
        movementAPI = ctx.api(MovementAPI.class);
        starSystemAPI = ctx.api(StarSystemAPI.class);

        return null;
    }
}
