package com.quicktable.restaurantservice.controller;

import com.quicktable.restaurantservice.dto.AdminRestaurantRequest;
import com.quicktable.restaurantservice.dto.RestaurantRequest;
import com.quicktable.restaurantservice.dto.RestaurantResponse;
import com.quicktable.restaurantservice.dto.TableRequest;
import com.quicktable.restaurantservice.dto.TableResponse;
import com.quicktable.restaurantservice.service.RestaurantService;
import com.quicktable.restaurantservice.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    private Long getUserIdFromRequest(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }

    private String getUserRoleFromRequest(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (role == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return role;
    }

    @Operation(summary = "[Restaurant Admin] Create restaurant", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(
            HttpServletRequest request,
            @Valid @RequestBody RestaurantRequest request2) {
        Long userId = getUserIdFromRequest(request);
        String userRole = getUserRoleFromRequest(request);
        RestaurantResponse response = restaurantService.createRestaurant(request2, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[System Admin] Create restaurant on behalf of owner", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @PostMapping("/admin")
    public ResponseEntity<RestaurantResponse> createRestaurantAsAdmin(
            HttpServletRequest request,
            @Valid @RequestBody AdminRestaurantRequest request2) {
        String userRole = getUserRoleFromRequest(request);
        if (!"SYSTEM_ADMIN".equals(userRole)) {
            throw new UnauthorizedException("Само системен администратор може да използва този endpoint.");
        }
        Long adminId = getUserIdFromRequest(request);
        RestaurantResponse response = restaurantService.createRestaurantAsAdmin(request2, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[Public] Get all restaurants", tags = {"Public"})
    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants(
            @RequestParam(required = false) String city
    ) {
        List<RestaurantResponse> restaurants = city != null
                ? restaurantService.getRestaurantsByCity(city)
                : restaurantService.getAllRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    @Operation(summary = "[Public] Get restaurant by ID", tags = {"Public"})
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse response = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Get my restaurants", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @GetMapping("/my")
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<RestaurantResponse> restaurants = restaurantService.getMyRestaurants(userId);
        return ResponseEntity.ok(restaurants);
    }

    @Operation(summary = "[System Admin] Get restaurants by user ID", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByUserId(
            HttpServletRequest request,
            @PathVariable Long userId
    ) {
        String userRole = getUserRoleFromRequest(request);
        if (!"SYSTEM_ADMIN".equals(userRole)) {
            throw new UnauthorizedException("Само системен администратор може да достъпва ресторантите на друг потребител.");
        }
        List<RestaurantResponse> restaurants = restaurantService.getMyRestaurants(userId);
        return ResponseEntity.ok(restaurants);
    }

    @Operation(summary = "[Restaurant Admin] Update restaurant", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
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

    @Operation(summary = "[Restaurant Admin] Update restaurant hours", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
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

    @Operation(summary = "[Restaurant Admin / System Admin] Delete restaurant", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
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

    @Operation(summary = "[Restaurant Admin] Add table to restaurant", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
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

    @Operation(summary = "[Public] Get restaurant tables", tags = {"Public"})
    @GetMapping("/{id}/tables")
    public ResponseEntity<List<TableResponse>> getRestaurantTables(@PathVariable Long id) {
        List<TableResponse> tables = restaurantService.getRestaurantTables(id);
        return ResponseEntity.ok(tables);
    }

    @Operation(summary = "[Client] Get available time slots", tags = {"Client"})
    @GetMapping("/{id}/available-time-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam Integer guestsCount
    ) {
        java.time.LocalDate reservationDate = java.time.LocalDate.parse(date);
        List<String> availableSlots = restaurantService.getAvailableTimeSlots(id, reservationDate, guestsCount);
        return ResponseEntity.ok(availableSlots);
    }

    @Operation(summary = "[Restaurant Admin] Update table availability", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
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

    @Operation(summary = "[Restaurant Admin] Toggle location availability", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PutMapping("/{id}/locations/{location}/toggle")
    public ResponseEntity<com.quicktable.restaurantservice.dto.LocationAvailabilityResponse> toggleLocationAvailability(
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
        com.quicktable.restaurantservice.dto.LocationAvailabilityResponse response =
                restaurantService.toggleLocationAvailability(id, tableLocation, enabled, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Public] Get location availability", tags = {"Public"})
    @GetMapping("/{id}/locations")
    public ResponseEntity<List<com.quicktable.restaurantservice.dto.LocationAvailabilityResponse>> getLocationAvailability(
            @PathVariable Long id
    ) {
        List<com.quicktable.restaurantservice.dto.LocationAvailabilityResponse> locations =
                restaurantService.getLocationAvailability(id);
        return ResponseEntity.ok(locations);
    }
}
