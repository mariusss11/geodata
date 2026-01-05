package com.geodata.controller;

import com.geodata.enums.UserRole;
import com.geodata.model.User;
import com.geodata.repository.UserRepository;
import com.geodata.service.AdminService;
import com.geodata.utils.ChangeRoleRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/api/admin")
@RestController
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdminService adminService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private void createUserIfNotExists(String usernameContains, String username, String password, UserRole role) {
        if (!userRepository.existsByUsernameContaining(usernameContains)) {
            if (isValidCredentials(username, password)) {
                User user = new User();
                user.setUsername(username);
                user.setName(role.name().toLowerCase());
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(role);
                userRepository.save(user);
                log.info("{} user created: {}", role.name(), username);
            } else {
                log.error("{} username or password is missing. Skipping {} creation.", role.name(), role.name().toLowerCase());
            }
        } else {
            log.info("The {} had already been created", role.name().toLowerCase());
        }
    }

    // Helper method to validate credentials
    private boolean isValidCredentials(String username, String password) {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/allEnabled")
    public List<User> getAllEnabledUsers() {
        return adminService.getAllEnableUsers();
    }

    @DeleteMapping("/disable")
    public ResponseEntity<String> deleteUserByAdmin(@RequestParam String email) {
        log.info("Admin is trying to disable the user with the email: {}", email);
        return ResponseEntity.ok(adminService.disableUserByEmail(email));
    }

    @PutMapping("/changeRole")
    public ResponseEntity<String> changeRoleToAnUser(@RequestBody ChangeRoleRequest request) {
        return adminService.changeRoleToAnUser(request);
    }

}
