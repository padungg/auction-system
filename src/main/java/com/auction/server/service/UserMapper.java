package com.auction.server.service;

import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.User;

/**
 * Chuyên trách chuyển đổi User entity → UserResponseDTO.
 * Tách ra khỏi UserService để tuân thủ SRP.
 */
public class UserMapper {

    /**
     * Chuyển User entity → UserResponseDTO đầy đủ (KHÔNG chứa password).
     */
    public UserResponseDTO toFullDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getStoreName(),
                user.getRating(),
                user.isActive()
        );
    }
}
