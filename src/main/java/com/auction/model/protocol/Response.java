package com.auction.model.protocol;

/**
 * Khung giao thức (Envelope) cho mọi kết quả trả về từ Server cho Client.
 */
public class Response {

    private ResponseStatus status;
    private String message;
    private Object payload; // Dữ liệu trả về (VD: danh sách UserResponseDTO)


    public Response() {
    }

    public Response(ResponseStatus status, String message, Object payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
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
}
