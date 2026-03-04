package com.quicktable.restaurantservice.repository;

import com.quicktable.common.dto.TableLocation;
import com.quicktable.restaurantservice.entity.LocationAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationAvailabilityRepository extends JpaRepository<LocationAvailability, Long> {
    
    List<LocationAvailability> findByRestaurantId(Long restaurantId);
    
    Optional<LocationAvailability> findByRestaurantIdAndLocation(Long restaurantId, TableLocation location);
    
    List<LocationAvailability> findByRestaurantIdAndEnabledTrue(Long restaurantId);
}
