package com.quicktable.reservationservice.service;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.UserRole;
import com.quicktable.reservationservice.client.RestaurantServiceClient;
import com.quicktable.reservationservice.dto.ReservationRequest;
import com.quicktable.reservationservice.dto.ReservationResponse;
import com.quicktable.reservationservice.dto.TableDTO;
import com.quicktable.reservationservice.dto.UpdateReservationStatusRequest;
import com.quicktable.reservationservice.entity.Reservation;
import com.quicktable.reservationservice.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantServiceClient restaurantServiceClient;

    @Transactional
    public ReservationResponse createReservation(Long userId, ReservationRequest request) {
        log.info("Създаване на резервация за потребител: {}", userId);

        // BUSINESS RULE: RESTAURANT_ADMIN не може да прави резервации
        if (request.getUserRole() == UserRole.RESTAURANT_ADMIN) {
            throw new RuntimeException("Администраторите на ресторанти не могат да правят резервации. Използвайте CLIENT акаунт.");
        }

        // АКО tableId НЕ Е ПОДАДЕНО, автоматично избираме подходяща маса
        if (request.getTableId() == null) {
            String location = request.getLocationPreference() != null ? 
                    request.getLocationPreference() : "ANY";
            
            Long autoSelectedTableId = findAvailableTable(
                    request.getRestaurantId(),
                    request.getReservationDate(),
                    request.getReservationTime(),
                    request.getGuestsCount(),
                    location
            );
            if (autoSelectedTableId == null) {
                String locationMsg = "ANY".equals(location) ? "" : " с локация " + location;
                throw new RuntimeException("Няма налична маса за избраното време, брой гости (" + 
                        request.getGuestsCount() + ")" + locationMsg);
            }
            request.setTableId(autoSelectedTableId);
            log.info("Автоматично избрана маса: {}", autoSelectedTableId);
        }

        // Проверка за наличност на масата
        List<Reservation> existingReservations = reservationRepository
                .findActiveReservationsForTable(request.getTableId(), request.getReservationDate());

        if (isTableOccupied(existingReservations, request.getReservationTime())) {
            throw new RuntimeException("Масата не е налична за избрания час");
        }

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .restaurantId(request.getRestaurantId())
                .tableId(request.getTableId())
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .numberOfGuests(request.getGuestsCount())
                .specialRequests(request.getSpecialRequests())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .status(ReservationStatus.PENDING)
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("Резервация създадена с ID: {}", reservation.getId());

        return mapToResponse(reservation);
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Резервация не е намерена"));
        return mapToResponse(reservation);
    }

    public List<ReservationResponse> getMyReservations(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    public List<ReservationResponse> getRestaurantReservations(Long restaurantId, Long adminUserId) {
        // TODO: Трябва да проверим через restaurant-service дали adminUserId е собственикът
        // За сега връщаме всички резервации за ресторанта
        log.info("Извличане на резервации за ресторант {} от админ {}", restaurantId, adminUserId);
        
        return reservationRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Намира първата налична маса за даден ресторант, дата, час и брой гости
     * @return tableId или null ако няма налична маса
     */
    private Long findAvailableTable(Long restaurantId, LocalDate date, LocalTime time, 
                                     Integer guestsCount, String locationPreference) {
        log.info("Търсене на налична маса за ресторант {} на {} в {} за {} гости (локация: {})",
                restaurantId, date, time, guestsCount, locationPreference);
        
        // Извличаме налични маси от restaurant-service
        List<TableDTO> availableTables = restaurantServiceClient.findAvailableTables(
                restaurantId, guestsCount, locationPreference);
        
        if (availableTables.isEmpty()) {
            log.warn("Няма налични маси с капацитет >= {} и локация {}", guestsCount, locationPreference);
            return null;
        }
        
        // Проверяваме коя маса е свободна за избраното време
        List<Reservation> existingReservations = reservationRepository
                .findByRestaurantIdAndReservationDate(restaurantId, date);
        
        for (TableDTO table : availableTables) {
            if (!isTableOccupied(table.getId(), time, existingReservations)) {
                log.info("Избрана маса {} (капацитет: {}, локация: {})", 
                        table.getTableNumber(), table.getCapacity(), table.getLocation());
                return table.getId();
            }
        }
        
        log.warn("Всички подходящи маси са заети за избраното време");
        return null;
    }
    
    private boolean isTableOccupied(Long tableId, LocalTime requestedTime, List<Reservation> existingReservations) {
        LocalTime endTime = requestedTime.plusHours(2);
        
        return existingReservations.stream()
                .filter(r -> r.getTableId().equals(tableId))
                .anyMatch(r -> {
                    LocalTime resTime = r.getReservationTime();
                    LocalTime resEndTime = resTime.plusHours(2);
                    
                    // Проверка за припокриване на времето
                    return !(resEndTime.isBefore(requestedTime) || resEndTime.equals(requestedTime) ||
                            resTime.isAfter(endTime) || resTime.equals(endTime));
                });
    }

    public List<String> getAvailableTimeSlots(Long restaurantId, LocalDate date, 
                                              Integer guestsCount, String locationPreference) {
        log.info("Извличане на налични часове за ресторант {} на дата {} за {} гости (локация: {})",
                restaurantId, date, guestsCount, locationPreference);

        // TODO: Извличаме работното време от restaurant-service
        // За сега генерираме слотове от 10:00 до 22:00
        List<LocalTime> allSlots = new ArrayList<>();
        for (int hour = 10; hour <= 22; hour++) {
            allSlots.add(LocalTime.of(hour, 0));
        }

        // Филтрираме заетите часове
        List<Reservation> todayReservations = reservationRepository
                .findByRestaurantIdAndReservationDate(restaurantId, date);

        // Проверяваме кои часове са свободни
        return allSlots.stream()
                .filter(slot -> hasAvailableTableForSlot(restaurantId, date, slot, guestsCount, 
                        locationPreference, todayReservations))
                .map(LocalTime::toString)
                .collect(Collectors.toList());
    }

    private boolean hasAvailableTableForSlot(Long restaurantId, LocalDate date, LocalTime time,
                                             Integer guestsCount, String locationPreference, 
                                             List<Reservation> existingReservations) {
        // Извличаме налични маси с подходящ капацитет и локация
        List<TableDTO> availableTables = restaurantServiceClient.findAvailableTables(
                restaurantId, guestsCount, locationPreference);
        
        if (availableTables.isEmpty()) {
            return false;
        }
        
        // Проверяваме дали поне една от масите е свободна за избрания час
        for (TableDTO table : availableTables) {
            if (!isTableOccupied(table.getId(), time, existingReservations)) {
                return true; // Поне една маса е свободна
            }
        }
        
        return false; // Всички маси са заети
    }

    public List<ReservationResponse> getRestaurantReservationsByStatus(
            Long restaurantId, 
            ReservationStatus status
    ) {
        return reservationRepository.findByRestaurantIdAndStatus(restaurantId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse updateReservationStatus(
            Long id, 
            UpdateReservationStatusRequest request
    ) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Резервация не е намерена"));

        reservation.setStatus(request.getStatus());
        reservation = reservationRepository.save(reservation);

        log.info("Статус на резервация {} променен на: {}", id, request.getStatus());
        return mapToResponse(reservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Резервация не е намерена"));

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new RuntimeException("Не може да се отмени завършена резервация");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("Резервация {} отменена", id);
    }

    public List<ReservationResponse> getAvailableTimeSlots(
            Long restaurantId,
            LocalDate date
    ) {
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForRestaurant(restaurantId, date);

        return activeReservations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean isTableOccupied(List<Reservation> reservations, LocalTime requestedTime) {
        // Проверяваме дали има резервация в рамките на 2 часа (стандартна продължителност)
        for (Reservation reservation : reservations) {
            LocalTime reservationEnd = reservation.getReservationTime().plusHours(2);
            LocalTime requestedEnd = requestedTime.plusHours(2);

            // Проверка за припокриване на времеви интервали
            if (!(requestedTime.isAfter(reservationEnd) || requestedEnd.isBefore(reservation.getReservationTime()))) {
                return true; // Масата е заета
            }
        }
        return false;
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .userId(reservation.getUserId())
                .restaurantId(reservation.getRestaurantId())
                .tableId(reservation.getTableId())
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .numberOfGuests(reservation.getNumberOfGuests())
                .status(reservation.getStatus())
                .specialRequests(reservation.getSpecialRequests())
                .customerName(reservation.getCustomerName())
                .customerPhone(reservation.getCustomerPhone())
                .customerEmail(reservation.getCustomerEmail())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
