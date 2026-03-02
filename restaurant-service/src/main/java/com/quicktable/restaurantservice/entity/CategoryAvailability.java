package com.quicktable.restaurantservice.entity;

import com.quicktable.common.dto.TableCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "category"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableCategory category;

    @Column(nullable = false)
    private Boolean enabled;

    @PrePersist
    protected void onCreate() {
        if (enabled == null) {
            enabled = true;
        }
    }
}
