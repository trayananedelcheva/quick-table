package com.quicktable.restaurantservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurant_tables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String tableNumber;

    @Column(nullable = false)
    private Integer capacity; // Брой места

    private String location; // Например: "Тераса", "Вътре", "До прозорец"

    @Column(nullable = false)
    private Boolean available;

    @PrePersist
    protected void onCreate() {
        if (available == null) {
            available = true;
        }
    }
}
