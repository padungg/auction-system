package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bộ điều phối trung tâm sự kiện đấu giá thời gian thực phía Client.
 */
public class AuctionEventDispatcher {

    private final CopyOnWriteArrayList<AuctionEventObserver> observers = new CopyOnWriteArrayList<>();
    private volatile Consumer<Runnable> taskExecutor = Platform::runLater;

    public void addObserver(AuctionEventObserver observer) {
        observers.addIfAbsent(observer);
    }

    public void removeObserver(AuctionEventObserver observer) {
        observers.remove(observer);
    }

    public void clear() {
        observers.clear();
    }

    public void setTaskExecutor(Consumer<Runnable> taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void dispatch(String eventName, String auctionId, JsonObject json) {
        for (AuctionEventObserver observer : observers) {
            taskExecutor.accept(() -> observer.onAuctionEvent(eventName, auctionId, json));
        }
    }
}