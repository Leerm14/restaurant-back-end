package com.nhahang.restaurant.repository;
import com.nhahang.restaurant.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Spring tự hiểu: "Tìm một User (Optional<User>) bằng trường email"
    Optional<User> findByEmail(String email);

    // Spring tự hiểu: "Tìm một User (Optional<User>) bằng trường phoneNumber"
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Spring tự hiểu: "Kiểm tra sự tồn tại (Boolean) bằng email"
    Boolean existsByEmail(String email);

    // Spring tự hiểu: "Kiểm tra sự tồn tại (Boolean) bằng phoneNumber"
    Boolean existsByPhoneNumber(String phoneNumber);
}