package com.quicktable.restaurantservice.controller;

import com.quicktable.restaurantservice.dto.RestaurantRequest;
import com.quicktable.restaurantservice.dto.RestaurantResponse;
import com.quicktable.restaurantservice.dto.TableRequest;
import com.quicktable.restaurantservice.dto.TableResponse;
import com.quicktable.restaurantservice.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Helper method за извличане на userId от JWT
    private Long getUserIdFromRequest(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("Authentication required");
        }
        return userId;
    }

    // Helper method за извличане на userRole от JWT
    private String getUserRoleFromRequest(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (role == null) {
            throw new RuntimeException("Authentication required");
        }
        return role;
    }

    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(
            HttpServletRequest request,
            @Valid @RequestBody RestaurantRequest request2) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        RestaurantResponse response = restaurantService.createRestaurant(request2, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants(
            @RequestParam(required = false) String city
    ) {
        List<RestaurantResponse> restaurants = city != null
                ? restaurantService.getRestaurantsByCity(city)
                : restaurantService.getAllRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse response = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<RestaurantResponse> restaurants = restaurantService.getMyRestaurants(userId);
        return ResponseEntity.ok(restaurants);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request2
    ) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        RestaurantResponse response = restaurantService.updateRestaurant(id, request2, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/hours")
    public ResponseEntity<RestaurantResponse> updateRestaurantHours(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam String openingTime,
            @RequestParam String closingTime
    ) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        RestaurantResponse response = restaurantService.updateRestaurantHours(
                id, userId, userRole,
                java.time.LocalTime.parse(openingTime),
                java.time.LocalTime.parse(closingTime)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean permanent
    ) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        
        if (permanent) {
            restaurantService.hardDeleteRestaurant(id, userId, userRole);
        } else {
            restaurantService.deleteRestaurant(id, userId, userRole);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tables")
    public ResponseEntity<TableResponse> addTable(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody TableRequest request2
    ) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        TableResponse response = restaurantService.addTable(id, request2, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/tables")
    public ResponseEntity<List<TableResponse>> getRestaurantTables(@PathVariable Long id) {
        List<TableResponse> tables = restaurantService.getRestaurantTables(id);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{id}/available-time-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(
            @PathVariable Long id,
            @RequestParam String date, // YYYY-MM-DD
            @RequestParam Integer guestsCount
    ) {
        java.time.LocalDate reservationDate = java.time.LocalDate.parse(date);
        List<String> availableSlots = restaurantService.getAvailableTimeSlots(id, reservationDate, guestsCount);
        return ResponseEntity.ok(availableSlots);
    }

    @PutMapping("/{restaurantId}/tables/{tableNumber}/availability")
    public ResponseEntity<TableResponse> updateTableAvailability(
            HttpServletRequest request,
            @PathVariable Long restaurantId,
            @PathVariable String tableNumber,
            @RequestParam Boolean available
    ) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        TableResponse response = restaurantService.updateTableAvailability(restaurantId, tableNumber, userId, userRole, available);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/locations/{location}/toggle")
    public ResponseEntity<Void> toggleLocationAvailability(
            HttpServletRequest request,
            @PathVariable Long id,
            @PathVariable String location,
            @RequestParam Boolean enabled
    ) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        
        com.quicktable.common.dto.TableLocation tableLocation;
        try {
            tableLocation = com.quicktable.common.dto.TableLocation.valueOf(location.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        restaurantService.toggleLocationAvailability(id, tableLocation, enabled, userId, userRole);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/locations")
    public ResponseEntity<List<com.quicktable.restaurantservice.dto.LocationAvailabilityResponse>> getLocationAvailability(
            @PathVariable Long id
    ) {
        List<com.quicktable.restaurantservice.dto.LocationAvailabilityResponse> locations = 
                restaurantService.getLocationAvailability(id);
        return ResponseEntity.ok(locations);
    }
}
