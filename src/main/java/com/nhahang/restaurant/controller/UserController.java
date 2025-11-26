package com.nhahang.restaurant.controller;
import com.nhahang.restaurant.dto.UserCreateRequest;
import com.nhahang.restaurant.dto.UserDTO;
import com.nhahang.restaurant.dto.UserUpdateRequest;
import com.nhahang.restaurant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    // --- API 0: LẤY THÔNG TIN NGƯỜI DÙNG HIỆN TẠI ---
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            UserDTO user = userService.getCurrentUser(authentication);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // --- API 1: LẤY TẤT CẢ NGƯỜI DÙNG ---
    @GetMapping
     @PreAuthorize("hasAuthority('READ_USER')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // --- API 2: LẤY NGƯỜI DÙNG THEO ID ---
    @GetMapping("/{id}")
     @PreAuthorize("hasAuthority('READ_USER')")
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
     @PreAuthorize("hasAuthority('READ_USER')")
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
     @PreAuthorize("hasAuthority('READ_USER')")
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
     @PreAuthorize("hasAuthority('READ_USER')")
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
     @PreAuthorize("hasAuthority('READ_USER')")
    public ResponseEntity<List<UserDTO>> getUsersByRoleName(@PathVariable String roleName) {
        try {
            List<UserDTO> users = userService.getUsersByRoleName(roleName);
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- API 7: TẠO NGƯỜI DÙNG MỚI ---
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
        try {
            UserDTO createdUser = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            System.err.println("--- BẮT ĐẦU LỖI TẠO USER ---");
            e.printStackTrace(); 
            System.err.println("--- KẾT THÚC LỖI TẠO USER ---");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
   // --- API 7: CẬP NHẬT NGƯỜI DÙNG ---
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_USER')")
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
     @PreAuthorize("hasAuthority('DELETE_USER')")
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
