package com.auction.model.entity;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private int warrantyMonths;

    public Electronics(String id, String name, String description, String condition, String sellerId,
            double startingPrice, String brand, int warrantyMonths) {
        super(id, name, description, condition, sellerId, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getDetailInfo() {
        return "Đồ điện tử: " + this.getName() + " Hãng: " + brand + " Bảo hành: " + warrantyMonths
                + "tháng Tình trạng: " + this.getCondition();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
