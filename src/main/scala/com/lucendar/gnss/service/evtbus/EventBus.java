/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */

package com.lucendar.gnss.service.evtbus;

import akka.actor.ActorRef;
import com.lucendar.gnss.sdk.almatt.FetchAlmAttReq;
import com.lucendar.gnss.sdk.gateway.OnlineOfflineNotif;
import info.gratour.jt808common.spi.model.CmdAsyncCompletedMsg;
import info.gratour.jt808common.spi.model.Event;
import info.gratour.jt808common.spi.model.TermCmd;
import info.gratour.jt808common.spi.model.TermCmdStateChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class EventBus {

    public static final Logger LOGGER = LoggerFactory.getLogger("gnss.eventBus");

    public static final AtomicReference<ActorRef> EventBusRef = new AtomicReference<>();

    public static ActorRef actor() {
        return EventBusRef.get();
    }

    public interface EventBusMsg {
    }

    public record PublishEvent<E>(String evtTyp, E evt) implements EventBusMsg {
    }

    public record RegisterListener<E>(EventListener<E> listener) implements EventBusMsg {
        public void unregister() {
            EventBus.send(new UnRegisterListener<>(listener));
        }
    }

    public record RegisterListenerActor(String evtTyp, ActorRef actor) implements EventBusMsg {
        public void unregister() {
            EventBus.send(new UnRegisterListenerActor(evtTyp, actor));
        }
    }

    public record UnRegisterListener<E>(EventListener<E> listener) implements EventBusMsg {
    }

    public record UnRegisterListenerActor(String evtTyp, ActorRef actor) implements EventBusMsg {
    }

    private static <T extends EventBusMsg> T send(T msg) {
        actor().tell(msg, ActorRef.noSender());
        return msg;
    }

    public static class EventHub<E> {
        public final String EventType;
        private final boolean printCallStack;

        public EventHub(Class<E> clzz, boolean printCallStack) {
            EventType = clzz.getSimpleName();
            this.printCallStack = printCallStack;
        }

        public EventHub(Class<E> clzz) {
            this(clzz, false);
        }

        public RegisterListener<E> register(EventListener<E> listener) {
            return send(new RegisterListener<>(listener));
        }


        public RegisterListenerActor register(ActorRef actor) {
            return send(new RegisterListenerActor(EventType, actor));
        }

        public void unregister(EventListener<E> listener) {
            send(new UnRegisterListener<>(listener));
        }

        public void unregister(ActorRef actor) {
            send(new UnRegisterListenerActor(EventType, actor));
        }

        public void publish(E e) {
            if (printCallStack) {
                Throwable t = new Throwable("Callstack print");
                LOGGER.debug("Publish message", t);
            }
            send(new PublishEvent<>(EventType, e));
        }
    }

    public static final EventHub<TermCmd> TermCmdEventHub = new EventHub<>(TermCmd.class);

    public static final EventHub<TermCmdStateChanged> CmdStateChangedEventHub =
            new EventHub<>(TermCmdStateChanged.class);

    public static final EventHub<OnlineOfflineNotif> OnlineOfflineNotifEventHub =
            new EventHub<>(OnlineOfflineNotif.class);

    public  static final EventHub<Event> EventEventHub = new EventHub<>(Event.class);

    public static final EventHub<CmdAsyncCompletedMsg> CmdAsyncCompletedEventHub =
            new EventHub<>(CmdAsyncCompletedMsg.class);

    public static final EventHub<FetchAlmAttReq> FetchAlmAttEventHub =
            new EventHub<>(FetchAlmAttReq.class);

}
