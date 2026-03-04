package com.quicktable.restaurantservice.entity;

import com.quicktable.common.dto.TableLocation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "location"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableLocation location;

    @Column(nullable = false)
    private Boolean enabled;

    @PrePersist
    protected void onCreate() {
        if (enabled == null) {
            enabled = true;
        }
    }
}
