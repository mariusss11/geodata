package com.geodata.service;

import com.geodata.model.User;
import com.geodata.utils.ChangeRoleRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminService {

    List<User> getAllUsers();

    List<User> getAllEnableUsers();

    String disableUserByEmail(String email);

    ResponseEntity<String> changeRoleToAnUser(ChangeRoleRequest request);
}
