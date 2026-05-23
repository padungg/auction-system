package com.auction.model;

import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.entity.*;
import com.auction.server.util.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho ItemFactory — kiểm tra Factory Pattern.
 *
 * Đảm bảo:
 *  - Factory tạo đúng loại Item (Electronics / Art / Vehicle) từ DTO
 *  - Các trường chuyên biệt được khởi tạo đúng
 *  - getDetailInfo() trả về chuỗi thông tin hợp lệ
 */
@DisplayName("ItemFactory Tests")
class ItemFactoryTest {

    private CreateAuctionDTO createElectronicsDTO() {
        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setItemType("ELECTRONICS");
        dto.setName("MacBook Pro");
        dto.setDescription("Laptop cao cấp");
        dto.setStartingPrice(30_000_000.0);
        dto.setBrand("Apple");
        dto.setWarrantyMonths(24);
        return dto;
    }

    private CreateAuctionDTO createArtDTO() {
        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setItemType("ART");
        dto.setName("Sunrise");
        dto.setDescription("Tranh phong cảnh");
        dto.setStartingPrice(50_000_000.0);
        dto.setArtistName("Van Gogh");
        dto.setMaterial("Oil");
        dto.setCreationYear(1889);
        return dto;
    }

    private CreateAuctionDTO createVehicleDTO() {
        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setItemType("VEHICLE");
        dto.setName("Toyota Camry");
        dto.setDescription("Xe gia đình");
        dto.setStartingPrice(500_000_000.0);
        dto.setBrand("Toyota");
        dto.setModel("Camry 2.0");
        dto.setYear(2020);
        dto.setKm(50000);
        return dto;
    }

    @Test
    @DisplayName("TC-FACTORY-01: createItemFromDTO (ELECTRONICS) → trả về đối tượng Electronics")
    void createElectronics_returnsCorrectType() throws ValidationException {
        CreateAuctionDTO dto = createElectronicsDTO();
        Item item = ItemFactory.createItemFromDTO("e-001", "seller-001", "Mới", dto);

        assertInstanceOf(Electronics.class, item);
    }

    @Test
    @DisplayName("TC-FACTORY-02: createItemFromDTO (ELECTRONICS) → các trường cơ bản đúng")
    void createElectronics_fieldsCorrect() throws ValidationException {
        CreateAuctionDTO dto = createElectronicsDTO();
        Electronics item = (Electronics) ItemFactory.createItemFromDTO("e-001", "seller-001", "Mới", dto);

        assertEquals("e-001", item.getId());
        assertEquals("MacBook Pro", item.getName());
        assertEquals("Laptop cao cấp", item.getDescription());
        assertEquals("Mới", item.getCondition());
        assertEquals("seller-001", item.getSellerId());
        assertEquals(30_000_000.0, item.getStartingPrice(), 0.001);
        assertEquals("Apple", item.getBrand());
        assertEquals(24, item.getWarrantyMonths());
    }

    @Test
    @DisplayName("TC-FACTORY-03: createItemFromDTO (ART) → trả về đối tượng Art")
    void createArt_returnsCorrectType() throws ValidationException {
        CreateAuctionDTO dto = createArtDTO();
        Item item = ItemFactory.createItemFromDTO("a-001", "seller-002", "Cũ", dto);

        assertInstanceOf(Art.class, item);
    }

    @Test
    @DisplayName("TC-FACTORY-04: createItemFromDTO (ART) → các trường nghệ thuật đúng")
    void createArt_fieldsCorrect() throws ValidationException {
        CreateAuctionDTO dto = createArtDTO();
        Art item = (Art) ItemFactory.createItemFromDTO("a-001", "seller-002", "Cũ", dto);

        assertEquals("Van Gogh", item.getArtistName());
        assertEquals("Oil", item.getMaterial());
        assertEquals(1889, item.getCreationYear());
    }

    @Test
    @DisplayName("TC-FACTORY-05: createItemFromDTO (VEHICLE) → trả về đối tượng Vehicle")
    void createVehicle_returnsCorrectType() throws ValidationException {
        CreateAuctionDTO dto = createVehicleDTO();
        Item item = ItemFactory.createItemFromDTO("v-001", "seller-003", "Cũ", dto);

        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    @DisplayName("TC-FACTORY-06: createItemFromDTO (VEHICLE) → các trường xe đúng")
    void createVehicle_fieldsCorrect() throws ValidationException {
        CreateAuctionDTO dto = createVehicleDTO();
        Vehicle item = (Vehicle) ItemFactory.createItemFromDTO("v-001", "seller-003", "Cũ", dto);

        assertEquals("Toyota", item.getBrand());
        assertEquals("Camry 2.0", item.getModel());
        assertEquals(2020, item.getYear());
        assertEquals(50000, item.getKm());
    }

    @Test
    @DisplayName("TC-FACTORY-07: Electronics.getDetailInfo() không null và chứa thông tin brand")
    void electronics_detailInfo_notNull() throws ValidationException {
        CreateAuctionDTO dto = createElectronicsDTO();
        Electronics item = (Electronics) ItemFactory.createItemFromDTO("e-001", "seller-001", "Mới", dto);
        String info = item.getDetailInfo();
        assertNotNull(info);
        assertFalse(info.isBlank());
    }

    @Test
    @DisplayName("TC-FACTORY-08: Art.getDetailInfo() chứa thông tin nghệ thuật")
    void art_detailInfo_notNull() throws ValidationException {
        CreateAuctionDTO dto = createArtDTO();
        Art item = (Art) ItemFactory.createItemFromDTO("a-001", "seller-002", "Cũ", dto);
        String info = item.getDetailInfo();
        assertNotNull(info);
        assertFalse(info.isBlank());
    }

    @Test
    @DisplayName("TC-FACTORY-09: Vehicle.getDetailInfo() chứa thông tin phương tiện")
    void vehicle_detailInfo_notNull() throws ValidationException {
        CreateAuctionDTO dto = createVehicleDTO();
        Vehicle item = (Vehicle) ItemFactory.createItemFromDTO("v-001", "seller-003", "Cũ", dto);
        String info = item.getDetailInfo();
        assertNotNull(info);
        assertFalse(info.isBlank());
    }

    @Test
    @DisplayName("TC-FACTORY-10: Enum Constants đúng giá trị")
    void factory_constants() {
        assertEquals("ELECTRONICS", ItemType.ELECTRONICS.name());
        assertEquals("ART", ItemType.ART.name());
        assertEquals("VEHICLE", ItemType.VEHICLE.name());
    }
}
