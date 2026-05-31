package com.jullyscraft.config;

import com.jullyscraft.entity.Role;
import com.jullyscraft.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initializeDefaultRoles() {
        log.info("Checking and seeding default roles...");

        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(role);
                log.info("Created default role: {}", roleName);
            } else {
                log.debug("Role already exists: {}", roleName);
            }
        }

        log.info("Default roles initialization completed");
    }
}
