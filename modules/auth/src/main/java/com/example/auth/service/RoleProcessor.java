package com.example.auth.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RoleProcessor {

    public String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "ROLE_USER";
        }

        return role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;
    }

}
