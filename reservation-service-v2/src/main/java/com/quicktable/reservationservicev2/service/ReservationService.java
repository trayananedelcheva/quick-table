package com.quicktable.reservationservicev2.service;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.TableLocation;
import com.quicktable.common.dto.UserRole;
import com.quicktable.reservationservicev2.client.RestaurantServiceClient;
import com.quicktable.reservationservicev2.client.UserServiceClient;
import com.quicktable.reservationservicev2.dto.ReservationRequest;
import com.quicktable.reservationservicev2.dto.ReservationResponse;
import com.quicktable.reservationservicev2.dto.RestaurantDTO;
import com.quicktable.reservationservicev2.dto.TableDTO;
import com.quicktable.reservationservicev2.dto.UserDTO;
import com.quicktable.reservationservicev2.entity.Reservation;
import com.quicktable.reservationservicev2.exception.InvalidReservationException;
import com.quicktable.reservationservicev2.exception.TableNotAvailableException;
import com.quicktable.reservationservicev2.notification.NotificationData;
import com.quicktable.reservationservicev2.notification.NotificationService;
import com.quicktable.reservationservicev2.notification.NotificationType;
import com.quicktable.reservationservicev2.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final UserServiceClient userServiceClient;
    private final NotificationService notificationService;
    private final Random random = new Random();

    @Transactional
    public ReservationResponse createReservation(Long userId, UserRole userRole, String token, ReservationRequest request) {

        // Само CLIENT може да прави резервации
        if (userRole != UserRole.CLIENT) {
            throw new InvalidReservationException("Само клиенти могат да правят резервации.");
        }

        // Резервацията трябва да е поне 1 час напред
        LocalDateTime reservationDateTime = LocalDateTime.of(request.getReservationDate(), request.getReservationTime());
        if (reservationDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new InvalidReservationException("Резервацията трябва да е направена поне 1 час предварително.");
        }

        // Валидация на часа спрямо работното време на ресторанта
        validateReservationTime(request.getRestaurantId(), request.getReservationTime());

        // Вземаме данните на потребителя от user-service
        UserDTO user = userServiceClient.getUserById(userId, token);

        // Намираме свободна маса
        Long tableId = findAvailableTable(
                request.getRestaurantId(),
                request.getReservationDate(),
                request.getReservationTime(),
                request.getGuestsCount(),
                request.getPreferredLocation()
        );

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .restaurantId(request.getRestaurantId())
                .tableId(tableId)
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .numberOfGuests(request.getGuestsCount())
                .preferredLocation(request.getPreferredLocation())
                .customerName(user.getFirstName() + " " + user.getLastName())
                .customerEmail(user.getEmail())
                .customerPhone(user.getPhoneNumber())
                .specialRequests(request.getSpecialRequests())
                .status(ReservationStatus.CONFIRMED)
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("Резервация {} създадена за потребител {}", reservation.getId(), userId);

        sendConfirmationEmail(reservation);

        return mapToResponse(reservation);
    }

    // ── Валидация на час спрямо работното време ────────────────────────────────

    private void validateReservationTime(Long restaurantId, LocalTime reservationTime) {
        // Часът трябва да е на кръгъл половин час (00 или 30 минути)
        if (reservationTime.getMinute() != 0 && reservationTime.getMinute() != 30) {
            throw new InvalidReservationException(
                    "Резервации могат да се правят само в начало на час или в половината (напр. 09:00, 09:30).");
        }

        RestaurantDTO restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
        if (restaurant == null || restaurant.getOpeningTime() == null || restaurant.getClosingTime() == null) {
            throw new InvalidReservationException("Не може да се определи работното време на ресторанта.");
        }

        LocalTime opening = restaurant.getOpeningTime();
        LocalTime lastSlot = restaurant.getClosingTime().minusHours(1);

        if (reservationTime.isBefore(opening)) {
            throw new InvalidReservationException(
                    "Ресторантът отваря в " + opening + ". Не може да резервирате преди това.");
        }

        if (reservationTime.isAfter(lastSlot)) {
            throw new InvalidReservationException(
                    "Последният възможен час за резервация е " + lastSlot +
                    " (1 час преди затваряне в " + restaurant.getClosingTime() + ").");
        }
    }

    // ── Намиране на свободна маса ─────────────────────────────────────────────

    private Long findAvailableTable(Long restaurantId, LocalDate date, LocalTime time,
                                    Integer guestsCount, TableLocation preferredLocation) {
        // 1. Всички маси с достатъчен капацитет и желана локация
        List<TableDTO> candidates = restaurantServiceClient.findTablesByCapacityAndLocation(
                restaurantId, guestsCount, preferredLocation);

        if (candidates.isEmpty()) {
            throw new TableNotAvailableException(
                    "Няма маса с капацитет за " + guestsCount + " гости" +
                    (preferredLocation != null ? " в локация " + preferredLocation.getDisplayName() : "") + ".");
        }

        // 2. Всички потвърдени резервации за този ресторант и дата
        List<Reservation> existing = reservationRepository
                .findActiveReservationsForRestaurant(restaurantId, date);

        // 3. Филтрираме само незаетите маси
        List<TableDTO> freeTables = candidates.stream()
                .filter(t -> !isTableOccupied(t.getId(), time, existing))
                .toList();

        if (freeTables.isEmpty()) {
            throw new TableNotAvailableException(
                    "Всички подходящи маси са заети за " + time + " на " + date + ".");
        }

        // 4. Случаен избор от свободните
        TableDTO selected = freeTables.get(random.nextInt(freeTables.size()));
        log.info("Избрана маса {} (капацитет: {}) от {} свободни",
                selected.getTableNumber(), selected.getCapacity(), freeTables.size());
        return selected.getId();
    }

    private boolean isTableOccupied(Long tableId, LocalTime requestedTime, List<Reservation> existing) {
        LocalTime requestedEnd = requestedTime.plusHours(2);

        return existing.stream()
                .filter(r -> r.getTableId().equals(tableId))
                .anyMatch(r -> {
                    LocalTime resEnd = r.getReservationTime().plusHours(2);
                    // Конфликт: интервалите [T, T+2h) се припокриват
                    return !(resEnd.compareTo(requestedTime) <= 0 || requestedEnd.compareTo(r.getReservationTime()) <= 0);
                });
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void sendConfirmationEmail(Reservation reservation) {
        try {
            String restaurantName = getRestaurantName(reservation.getRestaurantId());

            notificationService.sendEmail(NotificationData.builder()
                    .type(NotificationType.RESERVATION_CONFIRMED)
                    .recipientEmail(reservation.getCustomerEmail())
                    .recipientName(reservation.getCustomerName())
                    .reservationId(reservation.getId())
                    .restaurantName(restaurantName)
                    .reservationDate(reservation.getReservationDate())
                    .reservationTime(reservation.getReservationTime())
                    .numberOfGuests(reservation.getNumberOfGuests())
                    .specialRequests(reservation.getSpecialRequests())
                    .build());
        } catch (Exception e) {
            log.error("Грешка при изпращане на email за резервация {}: {}", reservation.getId(), e.getMessage());
        }
    }

    private String getRestaurantName(Long restaurantId) {
        try {
            var restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
            return restaurant != null ? restaurant.getName() : "Ресторант #" + restaurantId;
        } catch (Exception e) {
            return "Ресторант #" + restaurantId;
        }
    }

    // ── Извличане на резервация по ID ──────────────────────────────────────────

    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new InvalidReservationException("Резервация с ID " + reservationId + " не е намерена."));
        return mapToResponse(reservation);
    }

    // ── Извличане на резервации на потребител ─────────────────────────────────

    public List<ReservationResponse> getMyReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservationDateDesc(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private ReservationResponse mapToResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .restaurantId(r.getRestaurantId())
                .tableId(r.getTableId())
                .reservationDate(r.getReservationDate())
                .reservationTime(r.getReservationTime())
                .numberOfGuests(r.getNumberOfGuests())
                .status(r.getStatus())
                .preferredLocation(r.getPreferredLocation())
                .customerName(r.getCustomerName())
                .customerEmail(r.getCustomerEmail())
                .customerPhone(r.getCustomerPhone())
                .specialRequests(r.getSpecialRequests())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
