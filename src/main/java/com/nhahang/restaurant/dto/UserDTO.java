package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    private String uid;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
