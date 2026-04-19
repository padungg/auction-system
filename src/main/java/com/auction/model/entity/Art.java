package com.auction.model.entity;

public class Art extends Item{
    private String artistName, material;
    private int creationYear;


    public Art() {
    }

    public Art(String id, String name, String description, String condition, String sellerId, double startingPrice, String artistName, String material, int creationYear) {
        super(id, name, description, condition, sellerId, startingPrice);
        this.artistName = artistName;
        this.material = material;
        this.creationYear = creationYear;
    }

    @Override
    public String getDetailInfo(){
        return "Nghệ thuât: " + this.getName() + " Tác giả: " + artistName + " Chất liêu: " + material + " Tình trạng: " + this.getCondition();
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
}
