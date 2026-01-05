package com.geodata.enums;

import com.geodata.exceptions.InvalidRoleException;

public enum UserRole {
    ADMIN,
    USER,
    MANAGER;

    public static UserRole toRole(String role) {
        switch (role) {
            case "USER" -> {
                return UserRole.USER;
            }
            case "ADMIN" -> {
                return UserRole.ADMIN;
            }
            case "MANAGER" -> {
                return UserRole.MANAGER;
            }
            default -> throw new InvalidRoleException("Invalid role: " + role);
        }
    }
}