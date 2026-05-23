package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocketReaderTest {

  private Gson gson;
  private ConcurrentMap<String, CompletableFuture<Response>> pendingRequests;
  private AuctionEventDispatcher dispatcher;

  @BeforeEach
  public void setUp() {
    gson = GsonProvider.getInstance();
    pendingRequests = new ConcurrentHashMap<>();
    dispatcher = new AuctionEventDispatcher();
    // Configure to run task synchronously in test thread
    dispatcher.setTaskExecutor(Runnable::run);
  }

  @Test
  public void testReadResponseWithRequestId() throws Exception {
    String requestId = "req-123";
    CompletableFuture<Response> future = new CompletableFuture<>();
    pendingRequests.put(requestId, future);

    JsonObject jsonResponse = new JsonObject();
    jsonResponse.addProperty("status", "SUCCESS");
    jsonResponse.addProperty("message", "Success message");
    jsonResponse.addProperty("requestId", requestId);

    BufferedReader reader = new BufferedReader(new StringReader(jsonResponse.toString() + "\n"));
    SocketReader socketReader = new SocketReader(reader, gson, pendingRequests, dispatcher);

    socketReader.run();

    assertTrue(future.isDone());
    Response response = future.get();
    assertEquals(ResponseStatus.SUCCESS, response.getStatus());
    assertEquals("Success message", response.getMessage());
    assertEquals(requestId, response.getRequestId());
    assertTrue(pendingRequests.isEmpty());
  }

  @Test
  public void testReadResponseFallbackWithoutRequestId() throws Exception {
    String requestId = "req-123";
    CompletableFuture<Response> future = new CompletableFuture<>();
    pendingRequests.put(requestId, future);

    JsonObject jsonResponse = new JsonObject();
    jsonResponse.addProperty("status", "SUCCESS");
    jsonResponse.addProperty("message", "Success message");

    BufferedReader reader = new BufferedReader(new StringReader(jsonResponse.toString() + "\n"));
    SocketReader socketReader = new SocketReader(reader, gson, pendingRequests, dispatcher);

    socketReader.run();

    assertTrue(future.isDone());
    Response response = future.get();
    assertEquals(ResponseStatus.SUCCESS, response.getStatus());
    assertNull(response.getRequestId());
    assertTrue(pendingRequests.isEmpty());
  }

  @Test
  public void testReadEvent() throws Exception {
    JsonObject jsonEvent = new JsonObject();
    jsonEvent.addProperty("event", "BID_UPDATE");
    jsonEvent.addProperty("auctionId", "auc-999");
    jsonEvent.addProperty("price", 150000.0);

    final String[] receivedEvent = new String[1];
    final String[] receivedAuctionId = new String[1];
    final JsonObject[] receivedJson = new JsonObject[1];

    AuctionEventObserver observer = (event, auctionId, payload) -> {
      receivedEvent[0] = event;
      receivedAuctionId[0] = auctionId;
      receivedJson[0] = payload;
    };
    dispatcher.addObserver(observer);

    BufferedReader reader = new BufferedReader(new StringReader(jsonEvent.toString() + "\n"));
    SocketReader socketReader = new SocketReader(reader, gson, pendingRequests, dispatcher);

    socketReader.run();

    assertEquals("BID_UPDATE", receivedEvent[0]);
    assertEquals("auc-999", receivedAuctionId[0]);
    assertNotNull(receivedJson[0]);
    assertEquals(150000.0, receivedJson[0].get("price").getAsDouble());
    assertTrue(pendingRequests.isEmpty());
  }
}
