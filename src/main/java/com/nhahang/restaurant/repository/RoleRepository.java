package com.nhahang.restaurant.repository;
import com.nhahang.restaurant.model.entity.Role;
import com.nhahang.restaurant.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(RoleName roleName);
}
