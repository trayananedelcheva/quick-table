package com.quicktable.restaurantservice.repository;

import com.quicktable.common.dto.TableCategory;
import com.quicktable.restaurantservice.entity.CategoryAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAvailabilityRepository extends JpaRepository<CategoryAvailability, Long> {
    
    List<CategoryAvailability> findByRestaurantId(Long restaurantId);
    
    Optional<CategoryAvailability> findByRestaurantIdAndCategory(Long restaurantId, TableCategory category);
    
    List<CategoryAvailability> findByRestaurantIdAndEnabledTrue(Long restaurantId);
}
