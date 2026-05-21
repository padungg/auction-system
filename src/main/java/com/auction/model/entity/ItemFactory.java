package com.auction.model.entity;

import com.auction.model.dto.CreateAuctionDTO;
import com.auction.server.util.ValidationException;

public class ItemFactory {

    public static Item createItemFromDTO(String id, String sellerId, String condition, CreateAuctionDTO dto) throws ValidationException {
        if (dto.getItemType() == null) {
            throw new ValidationException("Thiếu loại sản phẩm");
        }

        ItemType type;
        try {
            type = ItemType.valueOf(dto.getItemType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Loại sản phẩm không hợp lệ. Chỉ chấp nhận: " + java.util.Arrays.toString(ItemType.values()));
        }

        switch (type) {
            case ELECTRONICS:
                return new Electronics(id, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getBrand(), dto.getWarrantyMonths());
            case ART:
                return new Art(id, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getArtistName(), dto.getMaterial(), dto.getCreationYear());
            case VEHICLE:
                return new Vehicle(id, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getBrand(), dto.getModel(), dto.getYear(), dto.getKm());
            default:
                throw new ValidationException("Loại sản phẩm không hợp lệ.");
        }
    }
}
