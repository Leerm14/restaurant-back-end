package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.UserCreateRequest;
import com.nhahang.restaurant.dto.UserDTO;
import com.nhahang.restaurant.dto.UserUpdateRequest;
import com.nhahang.restaurant.model.entity.Role;
import com.nhahang.restaurant.model.entity.User;
import com.nhahang.restaurant.repository.RoleRepository;
import com.nhahang.restaurant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Lấy tất cả người dùng
     */
    @Transactional
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả người dùng với phân trang
     */
    @Transactional
    public List<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy người dùng theo ID
     */
    @Transactional
    public UserDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        return convertToDTO(user);
    }

    /**
     * Lấy người dùng theo UID (Firebase)
     */
    @Transactional
    public UserDTO getUserByUid(String uid) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với UID: " + uid));
        return convertToDTO(user);
    }

    /**
     * Lấy người dùng theo email
     */
    @Transactional
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        return convertToDTO(user);
    }

    /**
     * Lấy người dùng theo số điện thoại
     */
    @Transactional
    public UserDTO getUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với số điện thoại: " + phoneNumber));
        return convertToDTO(user);
    }
    /**
     *  Lấy người dùng theo roleName
     */
    @Transactional
    public List<UserDTO> getUsersByRoleName(String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò: " + roleName));
        List<User> users = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().getId().equals(role.getId()))
                .collect(Collectors.toList());
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     *  Lấy người dùng theo roleName với phân trang
     */
    @Transactional
    public List<UserDTO> getUsersByRoleName(String roleName, int page, int size) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò: " + roleName));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);
        
        return userPage.getContent().stream()
                .filter(user -> user.getRole() != null && user.getRole().getId().equals(role.getId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Tạo người dùng mới
     */
    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        User user = new User();
        user.setUid(request.getUid());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        Role role;
        if (request.getRoleName() != null) {
            role = roleRepository.findByRoleName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò: " + request.getRoleName()));
        } else {
            role = roleRepository.findByRoleName("user")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò mặc định 'user'. Vui lòng kiểm tra database."));
        }
        user.setRole(role);

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Cập nhật thông tin người dùng
     */
    @Transactional
    public UserDTO updateUser(Integer id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(id)) {
                            throw new RuntimeException("Email đã được sử dụng: " + request.getEmail());
                        }
                    });
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(id)) {
                            throw new RuntimeException("Số điện thoại đã được sử dụng: " + request.getPhoneNumber());
                        }
                    });
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getRoleName() != null) {
            Role role = roleRepository.findByRoleName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò: " + request.getRoleName()));
            user.setRole(role);
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * Xóa người dùng
     */
    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        userRepository.delete(user);
    }

    /**
     * Chuyển đổi User entity sang UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUid(user.getUid());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName().name() : null);
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
