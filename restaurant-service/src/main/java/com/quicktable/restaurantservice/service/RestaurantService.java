package com.quicktable.restaurantservice.service;

import com.quicktable.common.dto.TableLocation;
import com.quicktable.restaurantservice.dto.*;
import com.quicktable.restaurantservice.entity.LocationAvailability;
import com.quicktable.restaurantservice.entity.Restaurant;
import com.quicktable.restaurantservice.entity.RestaurantTable;
import com.quicktable.restaurantservice.repository.LocationAvailabilityRepository;
import com.quicktable.restaurantservice.repository.RestaurantRepository;
import com.quicktable.restaurantservice.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final LocationAvailabilityRepository locationAvailabilityRepository;
    private final GeocodingService geocodingService;

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request, Long userId, String userRole) {
        log.info("Създаване на ресторант: {} от потребител {} с роля {}", request.getName(), userId, userRole);

        // BUSINESS RULE: Само RESTAURANT_ADMIN и SYSTEM_ADMIN могат да създават ресторанти
        if (!"RESTAURANT_ADMIN".equals(userRole) && !"SYSTEM_ADMIN".equals(userRole)) {
            throw new RuntimeException("Нямате права да създавате ресторант");
        }

        // Геокодиране на адреса с външна услуга (Nominatim API)
        GeoLocation geoLocation = geocodingService.geocodeAddress(request.getAddress());

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(geoLocation != null ? geoLocation.getCity() : request.getCity())
                .country(geoLocation != null ? geoLocation.getCountry() : request.getCountry())
                .latitude(geoLocation != null ? geoLocation.getLatitude() : null)
                .longitude(geoLocation != null ? geoLocation.getLongitude() : null)
                .phone(request.getPhone())
                .email(request.getEmail())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .adminUserId(userId) // Автоматично задаваме от JWT
                .active(true)
                .tables(new ArrayList<>())
                .build();

        // Добавяне на маси
        if (request.getTables() != null && !request.getTables().isEmpty()) {
            for (TableRequest tableRequest : request.getTables()) {
                RestaurantTable table = RestaurantTable.builder()
                        .restaurant(restaurant)
                        .tableNumber(tableRequest.getTableNumber())
                        .capacity(tableRequest.getCapacity())
                        .location(tableRequest.getLocation())
                        .available(true)
                        .build();
                restaurant.getTables().add(table);
            }
        }

        restaurant = restaurantRepository.save(restaurant);
        log.info("Ресторант създаден с ID: {}", restaurant.getId());

        // Автоматично създаване на LocationAvailability записи за всички локации
        for (TableLocation location : TableLocation.values()) {
            LocationAvailability locationAvailability = LocationAvailability.builder()
                    .restaurant(restaurant)
                    .location(location)
                    .enabled(true) // По подразбиране всички локации са активни
                    .build();
            locationAvailabilityRepository.save(locationAvailability);
        }

        return mapToResponse(restaurant, true);
    }

    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));
        return mapToResponse(restaurant, true);
    }

    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findByActiveTrue().stream()
                .map(r -> mapToResponse(r, false))
                .collect(Collectors.toList());
    }

    public List<RestaurantResponse> getRestaurantsByCity(String city) {
        return restaurantRepository.findByCityAndActiveTrue(city).stream()
                .map(r -> mapToResponse(r, false))
                .collect(Collectors.toList());
    }

    public List<RestaurantResponse> getMyRestaurants(Long adminUserId) {
        return restaurantRepository.findByAdminUserId(adminUserId).stream()
                .map(r -> mapToResponse(r, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request, Long userId, String userRole) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));

        // BUSINESS RULE: SYSTEM_ADMIN или собственик могат да редактират
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        if (!isSystemAdmin && !isOwner) {
            throw new RuntimeException("Нямате права да променяте този ресторант");
        }

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setOpeningTime(request.getOpeningTime());
        restaurant.setClosingTime(request.getClosingTime());

        // Ако адресът е променен, презокодираме
        if (!restaurant.getAddress().equals(request.getAddress())) {
            GeoLocation geoLocation = geocodingService.geocodeAddress(request.getAddress());
            if (geoLocation != null) {
                restaurant.setLatitude(geoLocation.getLatitude());
                restaurant.setLongitude(geoLocation.getLongitude());
                restaurant.setCity(geoLocation.getCity());
                restaurant.setCountry(geoLocation.getCountry());
            }
        }

        restaurant = restaurantRepository.save(restaurant);
        return mapToResponse(restaurant, true);
    }

    @Transactional
    public void deleteRestaurant(Long id, Long userId, String userRole) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));
        
        // BUSINESS RULE: SYSTEM_ADMIN или собственик могат да изтриват
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        if (!isSystemAdmin && !isOwner) {
            throw new RuntimeException("Нямате права да изтривате този ресторант");
        }
        
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
        log.info("Soft delete на ресторант {} (active=false) от {} потребител {}", 
                id, userRole, userId);
    }

    @Transactional
    public void hardDeleteRestaurant(Long id, Long userId, String userRole) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));
        
        // BUSINESS RULE: SYSTEM_ADMIN или собственик могат да правят hard delete
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        if (!isSystemAdmin && !isOwner) {
            throw new RuntimeException("Нямате права да изтривате окончателно този ресторант");
        }
        
        // Първо изтрий масите (заради foreign key constraint)
        restaurant.getTables().clear();
        restaurantRepository.save(restaurant);
        
        // След това изтрий ресторанта
        restaurantRepository.deleteById(id);
        log.info("Hard delete на ресторант {} от {} потребител {} - окончателно изтрит!", 
                id, userRole, userId);
    }

    @Transactional
    public TableResponse addTable(Long restaurantId, TableRequest request, Long userId, String userRole) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));

        // BUSINESS RULE: SYSTEM_ADMIN или собственик могат да добавят маса
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        if (!isSystemAdmin && !isOwner) {
            throw new RuntimeException("Нямате права да добавяте маси в този ресторант");
        }

        // VALIDATION: Проверка за дублиращ се tableNumber в същия ресторант
        tableRepository.findByRestaurantIdAndTableNumber(restaurantId, request.getTableNumber())
                .ifPresent(existingTable -> {
                    throw new RuntimeException(
                        String.format("Маса с номер '%s' вече съществува в този ресторант", 
                                      request.getTableNumber())
                    );
                });

        RestaurantTable table = RestaurantTable.builder()
                .restaurant(restaurant)
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .available(true)
                .build();

        table = tableRepository.save(table);
        return mapToTableResponse(table);
    }

    public List<TableResponse> getRestaurantTables(Long restaurantId) {
        return tableRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToTableResponse)
                .collect(Collectors.toList());
    }

    public List<String> getAvailableTimeSlots(Long restaurantId, java.time.LocalDate date, Integer guestsCount) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));

        // Намираме маси, които могат да поберат guestsCount гости
        List<RestaurantTable> suitableTables = tableRepository.findByRestaurantId(restaurantId).stream()
                .filter(RestaurantTable::getAvailable)
                .filter(table -> table.getCapacity() >= guestsCount)
                .collect(Collectors.toList());

        if (suitableTables.isEmpty()) {
            return Collections.emptyList();
        }

        // Генерираме всички възможни часове от opening до closing (на всеки час)
        List<String> allPossibleSlots = new ArrayList<>();
        java.time.LocalTime currentTime = restaurant.getOpeningTime();
        java.time.LocalTime closingTime = restaurant.getClosingTime().minusHours(2); // -2 часа за резервация

        while (currentTime.isBefore(closingTime) || currentTime.equals(closingTime)) {
            allPossibleSlots.add(currentTime.toString());
            currentTime = currentTime.plusHours(1);
        }

        log.info("Проверка за налични времеви слотове за ресторант {} на дата {} за {} гости",
                restaurantId, date, guestsCount);

        // Този endpoint само връща всички възможни часове според работното време
        // Reservation service ще провери заетостта и ще върне само свободните слотове
        return allPossibleSlots;
    }

    @Transactional
    public RestaurantResponse updateRestaurantHours(Long restaurantId, Long userId, String userRole,
                                                    java.time.LocalTime openingTime, 
                                                    java.time.LocalTime closingTime) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));

        // BUSINESS RULE: SYSTEM_ADMIN може да променя всички ресторанти
        //                RESTAURANT_ADMIN може да променя САМО своя ресторант (където е собственик)
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isRestaurantAdmin = "RESTAURANT_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        log.info("Проверка за права: userId={}, userRole={}, restaurantAdminUserId={}, isOwner={}", 
                userId, userRole, restaurant.getAdminUserId(), isOwner);
        
        // SYSTEM_ADMIN - може да променя всички ресторанти
        // RESTAURANT_ADMIN - може да променя САМО своя ресторант (където adminUserId == userId)
        if (!isSystemAdmin && !(isRestaurantAdmin && isOwner)) {
            throw new RuntimeException("Нямате права да променяте работното време на този ресторант. " +
                    "RESTAURANT_ADMIN може да променя само ресторанти, където е собственик.");
        }

        restaurant.setOpeningTime(openingTime);
        restaurant.setClosingTime(closingTime);
        restaurant = restaurantRepository.save(restaurant);

        log.info("Променено работно време на ресторант {}: {} - {} от потребител {} ({})", 
                restaurantId, openingTime, closingTime, userId, userRole);

        return mapToResponse(restaurant, false);
    }

    @Transactional
    public TableResponse updateTableAvailability(Long restaurantId, String tableNumber, Long userId, String userRole, Boolean available) {
        // Намираме ресторанта първо за да проверим правата
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));

        // BUSINESS RULE: SYSTEM_ADMIN или собственик могат да променят наличността на маси
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        if (!isSystemAdmin && !isOwner) {
            throw new RuntimeException("Нямате права да променяте маси на този ресторант");
        }

        // Търсим масата по ресторант + номер
        RestaurantTable table = tableRepository.findByRestaurantIdAndTableNumber(restaurantId, tableNumber)
                .orElseThrow(() -> new RuntimeException("Маса с номер " + tableNumber + " не е намерена в този ресторант"));

        table.setAvailable(available);
        table = tableRepository.save(table);

        log.info("Променена наличност на маса #{} в ресторант {}: {}", tableNumber, restaurantId, available);

        return mapToTableResponse(table);
    }

    private RestaurantResponse mapToResponse(Restaurant restaurant, boolean includeTables) {
        RestaurantResponse.RestaurantResponseBuilder builder = RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .city(restaurant.getCity())
                .country(restaurant.getCountry())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .phone(restaurant.getPhone())
                .email(restaurant.getEmail())
                .openingTime(restaurant.getOpeningTime())
                .closingTime(restaurant.getClosingTime())
                .adminUserId(restaurant.getAdminUserId())
                .active(restaurant.getActive())
                .totalTables(restaurant.getTables().size())
                .availableTables((int) restaurant.getTables().stream()
                        .filter(RestaurantTable::getAvailable).count());

        if (includeTables) {
            // Групиране на маси по локация
            java.util.Map<String, com.quicktable.restaurantservice.dto.LocationGroupResponse> locationsMap = new java.util.HashMap<>();
            
            // Извличаме enabled статусите за всички локации
            java.util.Map<com.quicktable.common.dto.TableLocation, Boolean> locationStatuses = new java.util.HashMap<>();
            List<LocationAvailability> availabilities = locationAvailabilityRepository.findByRestaurantId(restaurant.getId());
            for (LocationAvailability avail : availabilities) {
                locationStatuses.put(avail.getLocation(), avail.getEnabled());
            }
            
            // Групиране на маси по локация
            java.util.Map<com.quicktable.common.dto.TableLocation, List<TableResponse>> tablesByLocation = 
                restaurant.getTables().stream()
                    .collect(Collectors.groupingBy(
                        RestaurantTable::getLocation,
                        Collectors.mapping(this::mapToTableResponse, Collectors.toList())
                    ));
            
            // Създаване на LocationGroupResponse за всяка локация (дори ако няма маси)
            for (com.quicktable.common.dto.TableLocation location : com.quicktable.common.dto.TableLocation.values()) {
                List<TableResponse> tables = tablesByLocation.getOrDefault(location, new ArrayList<>());
                Boolean enabled = locationStatuses.getOrDefault(location, true);
                
                LocationGroupResponse locationGroup = LocationGroupResponse.builder()
                        .displayName(location.getDisplayName())
                        .enabled(enabled)
                        .tables(tables)
                        .build();
                
                locationsMap.put(location.name(), locationGroup);
            }
            
            builder.locations(locationsMap);
        }

        return builder.build();
    }

    private TableResponse mapToTableResponse(RestaurantTable table) {
        return TableResponse.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .location(table.getLocation())
                .available(table.getAvailable())
                .build();
    }

    // ==================== УПРАВЛЕНИЕ НА ЛОКАЦИИ ====================

    @Transactional
    public void toggleLocationAvailability(Long restaurantId, TableLocation location, Boolean enabled, 
                                          Long userId, String userRole) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Ресторант не е намерен"));

        // BUSINESS RULE: SYSTEM_ADMIN или собственикът могат да управляват локации
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isRestaurantAdmin = "RESTAURANT_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);

        if (!isSystemAdmin && !(isRestaurantAdmin && isOwner)) {
            throw new RuntimeException("Нямате права да управлявате локации за този ресторант");
        }

        LocationAvailability locationAvailability = locationAvailabilityRepository
                .findByRestaurantIdAndLocation(restaurantId, location)
                .orElseGet(() -> {
                    // Ако няма запис, създаваме нов
                    LocationAvailability newEntry = LocationAvailability.builder()
                            .restaurant(restaurant)
                            .location(location)
                            .enabled(enabled)
                            .build();
                    return locationAvailabilityRepository.save(newEntry);
                });

        locationAvailability.setEnabled(enabled);
        locationAvailabilityRepository.save(locationAvailability);

        log.info("Локация {} за ресторант {} е {} от потребител {}", 
                location, restaurantId, enabled ? "активирана" : "деактивирана", userId);
    }

    public List<com.quicktable.restaurantservice.dto.LocationAvailabilityResponse> getLocationAvailability(Long restaurantId) {
        return locationAvailabilityRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToLocationAvailabilityResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.quicktable.restaurantservice.dto.LocationAvailabilityResponse mapToLocationAvailabilityResponse(LocationAvailability entity) {
        return com.quicktable.restaurantservice.dto.LocationAvailabilityResponse.builder()
                .id(entity.getId())
                .restaurantId(entity.getRestaurant().getId())
                .location(entity.getLocation())
                .enabled(entity.getEnabled())
                .build();
    }

    public boolean isLocationEnabled(Long restaurantId, TableLocation location) {
        return locationAvailabilityRepository
                .findByRestaurantIdAndLocation(restaurantId, location)
                .map(LocationAvailability::getEnabled)
                .orElse(true); // По подразбиране локациите са активни
    }
}
