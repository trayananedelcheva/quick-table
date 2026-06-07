package com.quicktable.restaurantservice.controller;

import com.quicktable.common.dto.TableLocation;
import com.quicktable.restaurantservice.dto.AdminRestaurantRequest;
import com.quicktable.restaurantservice.dto.LocationAvailabilityResponse;
import com.quicktable.restaurantservice.dto.RestaurantRequest;
import com.quicktable.restaurantservice.dto.RestaurantResponse;
import com.quicktable.restaurantservice.dto.TableRequest;
import com.quicktable.restaurantservice.dto.TableResponse;
import com.quicktable.restaurantservice.exception.UnauthorizedException;
import com.quicktable.restaurantservice.security.SecurityUtils;
import com.quicktable.restaurantservice.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "[Restaurant Admin] Create restaurant", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest restaurantRequest) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        RestaurantResponse response = restaurantService.createRestaurant(restaurantRequest, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[System Admin] Create restaurant on behalf of owner", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @PostMapping("/admin")
    public ResponseEntity<RestaurantResponse> createRestaurantAsAdmin(
            @Valid @RequestBody AdminRestaurantRequest adminRestaurantRequest) {
        String userRole = securityUtils.getCurrentUserRole();
        if (!"SYSTEM_ADMIN".equals(userRole)) {
            throw new UnauthorizedException("Само системен администратор може да използва този endpoint.");
        }
        Long adminId = securityUtils.getCurrentUserId();
        RestaurantResponse response = restaurantService.createRestaurantAsAdmin(adminRestaurantRequest, adminId);
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
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants() {
        Long userId = securityUtils.getCurrentUserId();
        List<RestaurantResponse> restaurants = restaurantService.getMyRestaurants(userId);
        return ResponseEntity.ok(restaurants);
    }

    @Operation(summary = "[System Admin] Get restaurants by user ID", tags = {"System Admin"})
    @SecurityRequirement(name = "System Admin")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByUserId(@PathVariable Long userId) {
        String userRole = securityUtils.getCurrentUserRole();
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
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest restaurantRequest
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        RestaurantResponse response = restaurantService.updateRestaurant(id, restaurantRequest, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Update restaurant hours", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PutMapping("/{id}/hours")
    public ResponseEntity<RestaurantResponse> updateRestaurantHours(
            @PathVariable Long id,
            @RequestParam String openingTime,
            @RequestParam String closingTime
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        RestaurantResponse response = restaurantService.updateRestaurantHours(
                id, userId, userRole,
                LocalTime.parse(openingTime),
                LocalTime.parse(closingTime)
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin / System Admin] Delete restaurant", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean permanent
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
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
            @PathVariable Long id,
            @Valid @RequestBody TableRequest tableRequest
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        TableResponse response = restaurantService.addTable(id, tableRequest, userId, userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[Public] Get restaurant tables", tags = {"Public"})
    @GetMapping("/{id}/tables")
    public ResponseEntity<List<TableResponse>> getRestaurantTables(@PathVariable Long id) {
        List<TableResponse> tables = restaurantService.getRestaurantTables(id);
        return ResponseEntity.ok(tables);
    }

    @Operation(summary = "[Restaurant Admin] Update table", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PutMapping("/{restaurantId}/tables/{tableId}")
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId,
            @Valid @RequestBody TableRequest tableRequest
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        TableResponse response = restaurantService.updateTable(restaurantId, tableId, tableRequest, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Delete table", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @DeleteMapping("/{restaurantId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        restaurantService.deleteTable(restaurantId, tableId, userId, userRole);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "[Public] Get Google Maps link for restaurant", tags = {"Public"})
    @GetMapping("/{id}/maps-link")
    public ResponseEntity<Map<String, String>> getMapsLink(@PathVariable Long id) {
        String link = restaurantService.getMapsLink(id);
        return ResponseEntity.ok(Map.of("mapsLink", link));
    }

    @Operation(summary = "[Client] Get available time slots", tags = {"Client"})
    @GetMapping("/{id}/available-time-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam Integer guestsCount
    ) {
        LocalDate reservationDate = LocalDate.parse(date);
        List<String> availableSlots = restaurantService.getAvailableTimeSlots(id, reservationDate, guestsCount);
        return ResponseEntity.ok(availableSlots);
    }

    @Operation(summary = "[Restaurant Admin] Update table availability", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PutMapping("/{restaurantId}/tables/{tableNumber}/availability")
    public ResponseEntity<TableResponse> updateTableAvailability(
            @PathVariable Long restaurantId,
            @PathVariable String tableNumber,
            @RequestParam Boolean available
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        TableResponse response = restaurantService.updateTableAvailability(restaurantId, tableNumber, userId, userRole, available);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Restaurant Admin] Toggle location availability", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PutMapping("/{id}/locations/{location}/toggle")
    public ResponseEntity<LocationAvailabilityResponse> toggleLocationAvailability(
            @PathVariable Long id,
            @PathVariable String location,
            @RequestParam Boolean enabled
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        TableLocation tableLocation;
        try {
            tableLocation = TableLocation.valueOf(location.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        LocationAvailabilityResponse response =
                restaurantService.toggleLocationAvailability(id, tableLocation, enabled, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[Public] Get location availability", tags = {"Public"})
    @GetMapping("/{id}/locations")
    public ResponseEntity<List<LocationAvailabilityResponse>> getLocationAvailability(
            @PathVariable Long id
    ) {
        List<LocationAvailabilityResponse> locations =
                restaurantService.getLocationAvailability(id);
        return ResponseEntity.ok(locations);
    }

    @Operation(summary = "[Restaurant Admin] Upload restaurant image", tags = {"Restaurant Admin"})
    @SecurityRequirement(name = "Restaurant Admin")
    @PostMapping("/{id}/image")
    public ResponseEntity<RestaurantResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = securityUtils.getCurrentUserId();
        String userRole = securityUtils.getCurrentUserRole();
        RestaurantResponse response = restaurantService.uploadImage(id, file, userId, userRole);
        return ResponseEntity.ok(response);
    }
}
