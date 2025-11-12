package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    private String uid;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String roleName = "User";
}
