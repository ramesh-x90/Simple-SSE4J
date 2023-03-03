package org.simplesseclient.client;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
public class EventSource<T> {

    private final List<EventHandler<T>> syncHandlerList = Collections.synchronizedList(new ArrayList<>());
    private ExecutorService pool = Executors.newCachedThreadPool();
    private Iterator<T> eventTIterator;
    private boolean isAlive = false;


    public EventSource(Iterator<T> eventTIterator) {
        this.eventTIterator = eventTIterator;

    }

    public void open() {

        isAlive = true;

        if (pool.isTerminated()) {
            pool = Executors.newCachedThreadPool();
        }

        pool.submit(
                () -> {

                    while (eventTIterator.hasNext() && isAlive) {
                        if (Thread.currentThread().isInterrupted())
                            break;

                        T data = eventTIterator.next();

                        syncHandlerList.forEach(handler -> {
                            handler.handle(data);
                        });

                    }

                }

        );

    }

    public void addEventHandler(EventHandler<T> handler) {
        syncHandlerList.add(handler);
    }

    public void removeEventHandler(EventHandler<T> handler) {
        syncHandlerList.remove(handler);
    }

    public void close() {
        isAlive = false;

        if (!pool.isTerminated()) {
            this.pool.shutdown();
            this.pool.shutdownNow();
        }


    }


}
