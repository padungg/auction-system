package com.auction.server.dao;

import com.auction.model.entity.Art;
import com.auction.model.entity.Electronics;
import com.auction.model.entity.Item;
import com.auction.model.entity.Vehicle;
import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAOImpl implements ItemDAO {

    @Override
    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String itemType = rs.getString("item_type");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    String condition = rs.getString("condition_item");
                    String sellerId = rs.getString("seller_id");
                    double startingPrice = rs.getDouble("starting_price");
                    
                    if ("ART".equalsIgnoreCase(itemType)) {
                        String artistName = rs.getString("artist_name");
                        String material = rs.getString("material");
                        int creationYear = rs.getInt("creation_year");
                        return new Art(id, name, description, condition, sellerId, startingPrice, artistName, material, creationYear);
                        
                    } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
                        String brand = rs.getString("brand");
                        String model = rs.getString("model");
                        int year = rs.getInt("year");
                        int km = rs.getInt("km");
                        return new Vehicle(id, name, description, condition, sellerId, startingPrice, brand, model, year, km);
                        
                    } else if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
                        String brand = rs.getString("brand");
                        int warrantyMonths = rs.getInt("warranty_months");
                        return new Electronics(id, name, description, condition, sellerId, startingPrice, brand, warrantyMonths);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(">>> [ItemDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean save(Item item) {
        String sql = "INSERT INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, " +
                     "artist_name, material, creation_year, brand, model, year, km, warranty_months) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getCondition());
            stmt.setString(5, item.getSellerId());
            stmt.setDouble(6, item.getStartingPrice());
            
            // Default null values for subclass fields
            stmt.setString(8, null);
            stmt.setString(9, null);
            stmt.setObject(10, null);
            stmt.setString(11, null);
            stmt.setString(12, null);
            stmt.setObject(13, null);
            stmt.setObject(14, null);
            stmt.setObject(15, null);

            if (item instanceof Art) {
                stmt.setString(7, "ART");
                Art art = (Art) item;
                stmt.setString(8, art.getArtistName());
                stmt.setString(9, art.getMaterial());
                stmt.setInt(10, art.getCreationYear());
                
            } else if (item instanceof Vehicle) {
                stmt.setString(7, "VEHICLE");
                Vehicle vehicle = (Vehicle) item;
                stmt.setString(11, vehicle.getBrand());
                stmt.setString(12, vehicle.getModel());
                stmt.setInt(13, vehicle.getYear());
                stmt.setInt(14, vehicle.getKm());
                
            } else if (item instanceof Electronics) {
                stmt.setString(7, "ELECTRONICS");
                Electronics electronics = (Electronics) item;
                stmt.setString(11, electronics.getBrand());
                stmt.setInt(15, electronics.getWarrantyMonths());
                
            } else {
                stmt.setString(7, "UNKNOWN");
            }
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println(">>> [ItemDAO] Lỗi save: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean update(Item item) {
        String sql = "UPDATE items SET name = ?, description = ?, condition_item = ?, starting_price = ?, " +
                     "artist_name = ?, material = ?, creation_year = ?, brand = ?, model = ?, year = ?, km = ?, warranty_months = ? " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setString(3, item.getCondition());
            stmt.setDouble(4, item.getStartingPrice());

            // Default null values for subclass fields
            stmt.setString(5, null);
            stmt.setString(6, null);
            stmt.setObject(7, null);
            stmt.setString(8, null);
            stmt.setString(9, null);
            stmt.setObject(10, null);
            stmt.setObject(11, null);
            stmt.setObject(12, null);

            if (item instanceof Art) {
                Art art = (Art) item;
                stmt.setString(5, art.getArtistName());
                stmt.setString(6, art.getMaterial());
                stmt.setInt(7, art.getCreationYear());
            } else if (item instanceof Vehicle) {
                Vehicle vehicle = (Vehicle) item;
                stmt.setString(8, vehicle.getBrand());
                stmt.setString(9, vehicle.getModel());
                stmt.setInt(10, vehicle.getYear());
                stmt.setInt(11, vehicle.getKm());
            } else if (item instanceof Electronics) {
                Electronics electronics = (Electronics) item;
                stmt.setString(8, electronics.getBrand());
                stmt.setInt(12, electronics.getWarrantyMonths());
            }

            stmt.setString(13, item.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println(">>> [ItemDAO] Lỗi update: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println(">>> [ItemDAO] Lỗi delete: " + e.getMessage());
        }
        return false;
    }
}
