package ru.grim;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class ActorReceive extends AbstractActor {
    private final Map<String, Integer> container = new HashMap<>();
    private static final Integer DEFAULT_VALUE = -1;


    @Override
    public Receive createReceive() {
        return ReceiveBuilder
                .create()
                .match(Message.class, message -> {
                    getSender().tell(
                            container.getOrDefault(message.getUrl(), DEFAULT_VALUE),
                            ActorRef.noSender()
                    );
                })
                .match(
                        StoreMessage.class, storeMessage -> {
                            container.putIfAbsent(
                                    storeMessage.getUrl(),
                                    storeMessage.getTime());
                            })
                .build();
    }
}
