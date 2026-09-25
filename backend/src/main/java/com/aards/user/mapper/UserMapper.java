package com.aards.user.mapper;

import com.aards.user.User;
import com.aards.user.dto.UserDto;
import org.springframework.stereotype.Component;

// Converts User entity to DTO manually so passwords never go out.
@Component
public class UserMapper {

    public UserDto toDto(User entity) {
        if (entity == null) {
            return null;
        }
        return UserDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .departmentId(entity.getDepartmentId())
                .active(entity.isActive())
                .build();
    }
}
