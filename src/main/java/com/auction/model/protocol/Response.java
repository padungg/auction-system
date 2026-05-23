package com.auction.model.protocol;

/**
 * Khung giao thức cho mọi kết quả trả về từ Server cho Client.
 */
public class Response {

    private ResponseStatus status;
    private String message;
    private Object payload; // Dữ liệu trả về (VD: danh sách UserResponseDTO)
    private String requestId;


    public Response() {
    }

    public Response(ResponseStatus status, String message, Object payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    public Response(ResponseStatus status, String message, Object payload, String requestId) {
        this.status = status;
        this.message = message;
        this.payload = payload;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public <T> T getPayloadAs(Class<T> clazz) {
        if (payload == null) return null;
        com.google.gson.Gson gson = com.auction.model.util.GsonProvider.getInstance();
        return gson.fromJson(gson.toJsonTree(payload), clazz);
    }

    public <T> T getPayloadAs(com.google.gson.reflect.TypeToken<T> typeToken) {
        if (payload == null) return null;
        com.google.gson.Gson gson = com.auction.model.util.GsonProvider.getInstance();
        return gson.fromJson(gson.toJsonTree(payload), typeToken.getType());
    }
}
