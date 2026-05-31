package com.auction.model.entity;

import com.auction.model.dto.UpdateAuctionDTO;

/**
 * Thực thể đại diện cho phương tiện đi lại.
 * Kế thừa lớp Item và tự triển khai (override) logic cập nhật/thông tin riêng của mình.
 */
public class Vehicle extends Item{
    private String brand, model;
    private int year, km;


    public Vehicle() {
    }

    public Vehicle(String id, String name, String description, String condition, String sellerId, double startingPrice, String brand, String model, int year, int km) {
        super(id, name, description, condition, sellerId, startingPrice);
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.km = km;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.VEHICLE;
    }

    @Override
    public void applyUpdate(UpdateAuctionDTO dto) {
        if (dto.getBrand() != null && !dto.getBrand().trim().isEmpty())
            this.setBrand(dto.getBrand());
        if (dto.getModel() != null && !dto.getModel().trim().isEmpty())
            this.setModel(dto.getModel());
        if (dto.getYear() > 0)
            this.setYear(dto.getYear());
        if (dto.getKm() > 0)
            this.setKm(dto.getKm());
    }

    @Override
    public String getDetailInfo(){
        return "Phương tiện: " + this.getName() + "\nHãng xe: " + this.brand + "\nMẫu xe: " + model + "\nNăm sản xuất: " + year + "\nSố km đã đi: " + km + " km\nTình trạng: " + this.getCondition();
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

