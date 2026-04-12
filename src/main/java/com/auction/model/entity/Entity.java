package com.auction.model.entity;
import java.io.Serializable;
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;

    public Entity(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
