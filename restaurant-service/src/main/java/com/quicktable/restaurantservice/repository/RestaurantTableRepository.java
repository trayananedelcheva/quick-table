package com.quicktable.restaurantservice.repository;

import com.quicktable.common.dto.TableLocation;
import com.quicktable.restaurantservice.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    
    List<RestaurantTable> findByRestaurantId(Long restaurantId);
    
    List<RestaurantTable> findByRestaurantIdAndAvailableTrue(Long restaurantId);
    
    List<RestaurantTable> findByRestaurantIdAndCapacityGreaterThanEqual(Long restaurantId, Integer capacity);
    
    List<RestaurantTable> findByRestaurantIdAndLocationAndAvailableTrue(Long restaurantId, TableLocation location);
    
    List<RestaurantTable> findByRestaurantIdAndLocationAndCapacityGreaterThanEqualAndAvailableTrue(
            Long restaurantId, TableLocation location, Integer capacity);
    
    java.util.Optional<RestaurantTable> findByRestaurantIdAndTableNumber(Long restaurantId, String tableNumber);
}
