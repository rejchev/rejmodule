package ru.rejchev.rejmodule.events.base;

import eu.darkbot.api.events.Event;

public interface RejEvent extends Event {
    Object source();
}
