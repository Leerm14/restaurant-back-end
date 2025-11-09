package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.UserCreateRequest;
import com.nhahang.restaurant.dto.UserDTO;
import com.nhahang.restaurant.dto.UserUpdateRequest;
import com.nhahang.restaurant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    // --- API 1: LẤY TẤT CẢ NGƯỜI DÙNG ---
    @GetMapping
    @PreAuthorize("haspermission('READ_USER')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            // Validation
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            if (size > 100) size = 100;
            
            List<UserDTO> users = userService.getAllUsers(page, size);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // --- API 2: LẤY NGƯỜI DÙNG THEO ID ---
    @GetMapping("/{id}")
    @PreAuthorize("haspermission('READ_USER')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id) {
        try {
            UserDTO user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // --- API 3: LẤY NGƯỜI DÙNG THEO UID ---
    @GetMapping("/uid/{uid}")
    @PreAuthorize("haspermission('READ_USER')")
    public ResponseEntity<UserDTO> getUserByUid(@PathVariable String uid) {
        try {
            UserDTO user = userService.getUserByUid(uid);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- API 4: LẤY NGƯỜI DÙNG THEO EMAIL ---
    @GetMapping("/email/{email}")
    @PreAuthorize("haspermission('READ_USER')")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        try {
            UserDTO user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
 // --- API 5: LẤY NGƯỜI DÙNG THEO SỐ ĐIỆN THOẠI ---
    @GetMapping("/phone/{phoneNumber}")
    @PreAuthorize("haspermission('READ_USER')")
    public ResponseEntity<UserDTO> getUserByPhoneNumber(@PathVariable String phoneNumber) {
        try {
            UserDTO user = userService.getUserByPhoneNumber(phoneNumber);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // --- API 6: LẤY NGƯỜI DÙNG THEO ROLE NAME ---
    @GetMapping("/role/{roleName}")
    @PreAuthorize("haspermission('READ_USER')")
    public ResponseEntity<List<UserDTO>> getUsersByRoleName(
            @PathVariable String roleName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            // Validation
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            if (size > 100) size = 100;
            
            List<UserDTO> users = userService.getUsersByRoleName(roleName, page, size);
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- API 7: TẠO NGƯỜI DÙNG MỚI ---
    @PostMapping
    @PreAuthorize("haspermission('CREATE_USER')")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserCreateRequest request) {
        try {
            UserDTO createdUser = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
   // --- API 7: CẬP NHẬT NGƯỜI DÙNG ---
    @PutMapping("/{id}")
    @PreAuthorize("haspermission('UPDATE_USER')")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Integer id,
            @RequestBody UserUpdateRequest request) {
        try {
            UserDTO updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // --- API 8: XÓA NGƯỜI DÙNG ---
    @DeleteMapping("/{id}")
    @PreAuthorize("haspermission('DELETE_USER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
