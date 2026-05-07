package com.auction.model.entity;

public class ItemFactory {

    public static final String TYPE_ELECTRONICS = "ELECTRONICS";
    public static final String TYPE_ART = "ART";
    public static final String TYPE_VEHICLE = "VEHICLE";

    public static Electronics createElectronics(String id, String name, String description, String condition,
            String sellerId, double startingPrice,
            String brand, int warrantyMonths) {

        return new Electronics(id, name, description, condition, sellerId, startingPrice, brand, warrantyMonths);
    }

    public static Art createArt(String id, String name, String description, String condition,
            String sellerId, double startingPrice,
            String artistName, String material, int creationYear) {

        return new Art(id, name, description, condition, sellerId, startingPrice, artistName, material, creationYear);
    }

    public static Vehicle createVehicle(String id, String name, String description, String condition,
            String sellerId, double startingPrice,
            String brand, String model, int year, int km) {

        return new Vehicle(id, name, description, condition, sellerId, startingPrice, brand, model, year, km);
    }
}
