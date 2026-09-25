package com.aards.user.mapper;

import com.aards.user.User;
import com.aards.user.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User entity) {
        if (entity == null) {
            return null;
        }
        return UserDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .role(entity.getRole())
                .fullName(entity.getFullName())
                .department(entity.getDepartment())
                .enabled(entity.isEnabled())
                .build();
    }
}
