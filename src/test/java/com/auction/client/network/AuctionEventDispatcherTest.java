package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionEventDispatcherTest {

  @Test
  public void testDispatchNotification() {
    AuctionEventDispatcher dispatcher = new AuctionEventDispatcher();
    
    final int[] callCount1 = new int[1];
    final int[] callCount2 = new int[1];
    
    AuctionEventObserver observer1 = (event, auctionId, payload) -> callCount1[0]++;
    AuctionEventObserver observer2 = (event, auctionId, payload) -> callCount2[0]++;

    dispatcher.addObserver(observer1);
    dispatcher.addObserver(observer2);

    // Configure task executor to run synchronously in the current thread
    dispatcher.setTaskExecutor(Runnable::run);

    JsonObject json = new JsonObject();
    json.addProperty("event", "BID_UPDATE");

    dispatcher.dispatch("BID_UPDATE", "auc-1", json);

    assertEquals(1, callCount1[0]);
    assertEquals(1, callCount2[0]);
  }

  @Test
  public void testRemoveObserver() {
    AuctionEventDispatcher dispatcher = new AuctionEventDispatcher();
    
    final int[] callCount = new int[1];
    AuctionEventObserver observer = (event, auctionId, payload) -> callCount[0]++;

    dispatcher.addObserver(observer);
    dispatcher.removeObserver(observer);

    dispatcher.setTaskExecutor(Runnable::run);

    JsonObject json = new JsonObject();
    json.addProperty("event", "BID_UPDATE");

    dispatcher.dispatch("BID_UPDATE", "auc-1", json);

    assertEquals(0, callCount[0]);
  }
}
