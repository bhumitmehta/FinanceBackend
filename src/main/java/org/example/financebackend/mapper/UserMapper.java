package org.example.financebackend.mapper;

import org.example.financebackend.dto.response.UserResponse;
import org.example.financebackend.model.User;
import org.example.financebackend.model.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(user.getUserRoles().stream().map(ur -> ur.getRole().getName()).collect(java.util.stream.Collectors.toSet()))")
    UserResponse toResponse(User user);
}
