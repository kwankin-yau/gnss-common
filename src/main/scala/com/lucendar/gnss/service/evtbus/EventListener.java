package com.lucendar.gnss.service.evtbus;

public interface EventListener<E> {

    /**
     * Event type.
     *
     * @return
     */
    String evtTyp();

    void onEvent(E e);
}
