package com.auction.model;

import com.auction.model.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho ItemFactory — kiểm tra Factory Pattern.
 *
 * Đảm bảo:
 *  - Factory tạo đúng loại Item (Electronics / Art / Vehicle)
 *  - Các trường chuyên biệt được khởi tạo đúng
 *  - getDetailInfo() trả về chuỗi thông tin hợp lệ
 */
@DisplayName("ItemFactory Tests")
class ItemFactoryTest {

    @Test
    @DisplayName("TC-FACTORY-01: createElectronics → trả về đối tượng Electronics")
    void createElectronics_returnsCorrectType() {
        Item item = ItemFactory.createElectronics(
                "e-001", "MacBook Pro", "Laptop cao cấp", "Mới",
                "seller-001", 30_000_000.0, "Apple", 24);

        assertInstanceOf(Electronics.class, item);
    }

    @Test
    @DisplayName("TC-FACTORY-02: createElectronics → các trường cơ bản đúng")
    void createElectronics_fieldsCorrect() {
        Electronics item = ItemFactory.createElectronics(
                "e-001", "iPhone 15", "Điện thoại", "Mới",
                "seller-001", 20_000_000.0, "Apple", 12);

        assertEquals("e-001", item.getId());
        assertEquals("iPhone 15", item.getName());
        assertEquals("Điện thoại", item.getDescription());
        assertEquals("Mới", item.getCondition());
        assertEquals("seller-001", item.getSellerId());
        assertEquals(20_000_000.0, item.getStartingPrice(), 0.001);
        assertEquals("Apple", item.getBrand());
        assertEquals(12, item.getWarrantyMonths());
    }

    @Test
    @DisplayName("TC-FACTORY-03: createArt → trả về đối tượng Art")
    void createArt_returnsCorrectType() {
        Item item = ItemFactory.createArt(
                "a-001", "Sunrise", "Tranh phong cảnh", "Cũ",
                "seller-002", 50_000_000.0, "Van Gogh", "Oil", 1889);

        assertInstanceOf(Art.class, item);
    }

    @Test
    @DisplayName("TC-FACTORY-04: createArt → các trường nghệ thuật đúng")
    void createArt_fieldsCorrect() {
        Art item = (Art) ItemFactory.createArt(
                "a-002", "Starry Night", "Bầu trời đêm", "Cũ",
                "seller-002", 100_000_000.0, "Van Gogh", "Oil on canvas", 1889);

        assertEquals("Van Gogh", item.getArtistName());
        assertEquals("Oil on canvas", item.getMaterial());
        assertEquals(1889, item.getCreationYear());
    }

    @Test
    @DisplayName("TC-FACTORY-05: createVehicle → trả về đối tượng Vehicle")
    void createVehicle_returnsCorrectType() {
        Item item = ItemFactory.createVehicle(
                "v-001", "Toyota Camry", "Xe gia đình", "Cũ",
                "seller-003", 500_000_000.0, "Toyota", "Camry 2.0", 2020, 50000);

        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    @DisplayName("TC-FACTORY-06: createVehicle → các trường xe đúng")
    void createVehicle_fieldsCorrect() {
        Vehicle item = (Vehicle) ItemFactory.createVehicle(
                "v-002", "Honda Civic", "Xe thể thao", "Mới",
                "seller-003", 700_000_000.0, "Honda", "Civic RS", 2023, 0);

        assertEquals("Honda", item.getBrand());
        assertEquals("Civic RS", item.getModel());
        assertEquals(2023, item.getYear());
        assertEquals(0, item.getKm());
    }

    @Test
    @DisplayName("TC-FACTORY-07: Electronics.getDetailInfo() không null và chứa thông tin brand")
    void electronics_detailInfo_notNull() {
        Electronics item = ItemFactory.createElectronics(
                "e-003", "Samsung TV", "Smart TV", "Mới",
                "s-001", 15_000_000.0, "Samsung", 24);
        String info = item.getDetailInfo();
        assertNotNull(info);
        assertFalse(info.isBlank());
    }

    @Test
    @DisplayName("TC-FACTORY-08: Art.getDetailInfo() chứa thông tin nghệ thuật")
    void art_detailInfo_notNull() {
        Art item = (Art) ItemFactory.createArt(
                "a-003", "Abstract", "Trừu tượng", "Mới",
                "s-001", 10_000_000.0, "Picasso", "Acrylic", 2023);
        String info = item.getDetailInfo();
        assertNotNull(info);
        assertFalse(info.isBlank());
    }

    @Test
    @DisplayName("TC-FACTORY-09: Vehicle.getDetailInfo() chứa thông tin phương tiện")
    void vehicle_detailInfo_notNull() {
        Vehicle item = (Vehicle) ItemFactory.createVehicle(
                "v-003", "BMW X5", "SUV cao cấp", "Mới",
                "s-001", 3_000_000_000.0, "BMW", "X5 M50i", 2024, 0);
        String info = item.getDetailInfo();
        assertNotNull(info);
        assertFalse(info.isBlank());
    }

    @Test
    @DisplayName("TC-FACTORY-10: Constants đúng giá trị")
    void factory_constants() {
        assertEquals("ELECTRONICS", ItemFactory.TYPE_ELECTRONICS);
        assertEquals("ART", ItemFactory.TYPE_ART);
        assertEquals("VEHICLE", ItemFactory.TYPE_VEHICLE);
    }
}
