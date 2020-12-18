package ru.grim;


import akka.actor.AbstractActor;

import java.util.HashMap;
import java.util.Map;

public class ActorReceive extends AbstractActor {
    private final Map<String, Integer> container = new HashMap<>();

    @Override
    public Receive createReceive() {
        return null;
    }
}
