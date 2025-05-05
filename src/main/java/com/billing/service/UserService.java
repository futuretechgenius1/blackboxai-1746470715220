package com.billing.service;

import com.billing.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    // User CRUD operations
    User createUser(User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    User getUserById(Long id);
    User getUserByEmail(String email);
    List<User> getAllUsers();
    Page<User> getAllUsers(Pageable pageable);
    
    // Role management
    User assignRole(Long userId, String roleName);
    User removeRole(Long userId, String roleName);
    
    // Password management
    void changePassword(Long userId, String currentPassword, String newPassword);
    void resetPassword(String email);
    
    // User status management
    void enableUser(Long userId);
    void disableUser(Long userId);
    
    // Validation methods
    boolean isEmailUnique(String email);
    boolean validatePassword(String password);
}
