package com.auction.model.protocol;

/**
 * Khung giao thức (Envelope) cho mọi yêu cầu gửi từ Client lên Server.
 */
public class Request {

    private RequestType type;
    private Object payload; // Chứa DTO tùy theo type (vd: LoginDTO, BidRequestDTO)

    public Request(RequestType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
