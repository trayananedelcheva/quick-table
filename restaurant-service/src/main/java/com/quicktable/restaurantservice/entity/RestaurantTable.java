package com.quicktable.restaurantservice.entity;

import com.quicktable.common.dto.TableLocation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "restaurant_tables",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_restaurant_table_number",
            columnNames = {"restaurant_id", "table_number"}
        )
    }
)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableLocation location; // Локация: INSIDE, SUMMER_GARDEN, WINTER_GARDEN

    @Column(nullable = false)
    private Boolean available; // Дали масата е отворена (за ремонт и т.н.)

    @PrePersist
    protected void onCreate() {
        if (available == null) {
            available = true;
        }
    }
}
