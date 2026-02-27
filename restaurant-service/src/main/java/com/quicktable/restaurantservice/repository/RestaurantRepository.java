package com.quicktable.restaurantservice.repository;

import com.quicktable.restaurantservice.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    
    List<Restaurant> findByActiveTrue();
    
    List<Restaurant> findByCityAndActiveTrue(String city);
    
    List<Restaurant> findByAdminUserId(Long adminUserId);
}
