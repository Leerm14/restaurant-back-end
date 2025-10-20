package com.nhahang.restaurant.model.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    // ----- Quan hệ ngược -----
    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<MenuItem> menuItems;
}
