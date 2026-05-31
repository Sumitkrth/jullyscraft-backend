package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.UserResponse;
import com.jullyscraft.dto.response.UserSummaryResponse;
import com.jullyscraft.entity.Role;
import com.jullyscraft.entity.User;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserResponse toResponse(User user);

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserSummaryResponse toSummary(User user);

    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
    }
}