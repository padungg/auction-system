package com.auction.model.dto;

/**
 * Gói tin DTO Client gửi lên khi muốn Đăng Bán (Tạo phiến đấu giá mới).
 * DTO này là dạng phẳng (Flattened DTO) gộp tất cả tham số của các loại Item.
 * Phía Server sẽ dựa vào itemType để trích xuất ra đúng tham số cần thiết.
 */
public class CreateAuctionDTO {

    private String itemType; // Sẽ truyền "ART", "ELECTRONICS", hoặc "VEHICLE"
    private String name;
    private String description;
    private String condition;
    private double startingPrice;
    private int durationDays; // Phiên đấu giá diễn ra bao nhiêu ngày?

    // --- CÁC TRƯỜNG CỦA ART ---
    private String artistName;
    private String material;
    private int creationYear;

    // --- CÁC TRƯỜNG CỦA ELECTRONICS ---
    private String brand;
    private int warrantyMonths;

    // --- CÁC TRƯỜNG CỦA VEHICLE ---
    private String model;
    private int year;
    private int km;

    public CreateAuctionDTO() {
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public void setCreationYear(int creationYear) {
        this.creationYear = creationYear;
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
