package com.auction.model.entity;

public class Vehicle extends Item{
    private String brand, model;
    private int year, km;

    public Vehicle(String id, String name, String description, String condition, String sellerId, double startingPrice, String brand, String model, int year, int km) {
        super(id, name, description, condition, sellerId, startingPrice);
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.km = km;
    }

    @Override
    public String getDetailInfo(){
        return "Phương tiện: " + this.brand + " " + model + " " + year + " Đã đi: " + km + "km Tình trạng: " + this.getCondition();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getKm() {
        return km;
    }

    public void setKm(int km) {
        this.km = km;
    }
}
