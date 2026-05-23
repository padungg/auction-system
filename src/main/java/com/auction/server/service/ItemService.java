package com.auction.server.service;

import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.entity.Item;
import com.auction.model.entity.ItemFactory;
import com.auction.server.dao.ItemDAO;
import com.auction.server.util.ValidationException;

import java.util.UUID;

/**
 * Service chuyên trách quản lý vòng đời của Sản phẩm (Item).
 */
public class ItemService {
    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public Item createItem(CreateAuctionDTO dto, String sellerId) throws ValidationException {
        String itemId = UUID.randomUUID().toString();
        String condition = dto.getCondition() != null ? dto.getCondition() : "Mới";

        Item item = ItemFactory.createItemFromDTO(itemId, sellerId, condition, dto);
        item.setImageBase64(dto.getImageBase64());

        itemDAO.save(item);
        return item;
    }

    public void updateItem(String itemId, UpdateAuctionDTO dto, String sellerId) throws ValidationException {
        Item item = itemDAO.findById(itemId);
        if (item == null || !item.getSellerId().equals(sellerId)) {
            throw new ValidationException("Bạn không có quyền sửa sản phẩm này");
        }

        if (dto.getName() != null && !dto.getName().trim().isEmpty())
            item.setName(dto.getName());
        if (dto.getDescription() != null)
            item.setDescription(dto.getDescription());
        if (dto.getCondition() != null)
            item.setCondition(dto.getCondition());
        if (dto.getStartingPrice() > 0) {
            item.setStartingPrice(dto.getStartingPrice());
        }

        item.applyUpdate(dto);
        itemDAO.update(item);
    }

    public void deleteItem(String itemId, String sellerId) throws ValidationException {
        Item item = itemDAO.findById(itemId);
        if (item == null || !item.getSellerId().equals(sellerId)) {
            throw new ValidationException("Bạn không có quyền xóa sản phẩm này");
        }
        itemDAO.delete(itemId);
    }
}
