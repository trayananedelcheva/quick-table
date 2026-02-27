package com.quicktable.restaurantservice.service;

import com.quicktable.restaurantservice.dto.*;
import com.quicktable.restaurantservice.entity.Restaurant;
import com.quicktable.restaurantservice.entity.RestaurantTable;
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
    public TableResponse updateTableAvailability(Long tableId, Long userId, String userRole, Boolean available) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Маса не е намерена"));

        Restaurant restaurant = table.getRestaurant();

        // BUSINESS RULE: SYSTEM_ADMIN или собственик могат да променят наличността на маси
        boolean isSystemAdmin = "SYSTEM_ADMIN".equals(userRole);
        boolean isOwner = restaurant.getAdminUserId().equals(userId);
        
        if (!isSystemAdmin && !isOwner) {
            throw new RuntimeException("Нямате права да променяте маси на този ресторант");
        }

        table.setAvailable(available);
        table = tableRepository.save(table);

        log.info("Променена наличност на маса {}: {}", tableId, available);

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
            List<TableResponse> tables = restaurant.getTables().stream()
                    .map(this::mapToTableResponse)
                    .collect(Collectors.toList());
            builder.tables(tables);
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
}
