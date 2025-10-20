package com.nhahang.restaurant.repository;
import com.nhahang.restaurant.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Spring Data JPA tự động tạo query tìm Role bằng roleName
    Optional<Role> findByRoleName(String roleName);
}
