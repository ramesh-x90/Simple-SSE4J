package org.simplesseclient.client;


@FunctionalInterface
public interface EventHandler<T> {
    void handle(T data);
}
