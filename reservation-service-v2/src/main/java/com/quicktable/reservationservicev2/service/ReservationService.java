package com.quicktable.reservationservicev2.service;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.TableLocation;
import com.quicktable.common.dto.UserRole;
import com.quicktable.reservationservicev2.client.RestaurantServiceClient;
import com.quicktable.reservationservicev2.client.UserServiceClient;
import com.quicktable.reservationservicev2.dto.AdminReservationRequest;
import com.quicktable.reservationservicev2.dto.TimeSlotResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final UserServiceClient userServiceClient;
    private final NotificationService notificationService;
@Value("${app.base-url}")
    private String baseUrl;

    private static final String RESERVATION_NOT_FOUND = "Резервация с ID ";
    private static final String NOT_FOUND_SUFFIX = " не е намерена.";

    @Transactional
    public ReservationResponse createReservation(Long userId, UserRole userRole, String token, ReservationRequest request) {

        if (userRole != UserRole.CLIENT) {
            throw new InvalidReservationException("Само клиенти могат да правят резервации.");
        }

        LocalDateTime reservationDateTime = LocalDateTime.of(request.getReservationDate(), request.getReservationTime());
        if (reservationDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new InvalidReservationException("Резервацията трябва да е направена поне 1 час предварително.");
        }

        validateReservationTime(request.getRestaurantId(), request.getReservationTime());
        validateNoOverlappingReservation(userId, request.getReservationDate(), request.getReservationTime());

        UserDTO user = userServiceClient.getUserById(userId, token);

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

    @Transactional
    public ReservationResponse createReservationAsAdmin(Long adminId, String token, AdminReservationRequest request) {
        UserDTO user = userServiceClient.getUserById(request.getTargetUserId(), token);

        Long tableId = findAvailableTable(
                request.getRestaurantId(),
                request.getReservationDate(),
                request.getReservationTime(),
                request.getGuestsCount(),
                request.getPreferredLocation()
        );

        Reservation reservation = Reservation.builder()
                .userId(request.getTargetUserId())
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
        log.info("Резервация {} създадена от администратор {} за потребител {}", reservation.getId(), adminId, request.getTargetUserId());

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

    // ── Проверка за припокриващи се резервации на потребителя ────────────────

    private void validateNoOverlappingReservation(Long userId, LocalDate date, LocalTime newTime) {
        List<Reservation> existing = reservationRepository.findConfirmedReservationsByUserAndDate(userId, date);

        LocalTime windowStart = newTime.minusMinutes(90);
        LocalTime windowEnd = newTime.plusMinutes(90);

        boolean hasConflict = existing.stream().anyMatch(r -> {
            LocalTime t = r.getReservationTime();
            return t.isAfter(windowStart) && t.isBefore(windowEnd);
        });

        if (hasConflict) {
            throw new InvalidReservationException(
                    "Вече имате резервация в рамките на 1.5 часа от желания час. " +
                    "Не може да направите две резервации едновременно.");
        }
    }

    // ── Намиране на свободна маса ─────────────────────────────────────────────

    private Long findAvailableTable(Long restaurantId, LocalDate date, LocalTime time,
                                    Integer guestsCount, TableLocation preferredLocation) {
        // 1. Маси с достатъчен капацитет в желаната зона
        List<TableDTO> candidates = restaurantServiceClient.findTablesByCapacityAndLocation(
                restaurantId, guestsCount, preferredLocation);

        if (candidates.isEmpty()) {
            throw new TableNotAvailableException(
                    "Няма маса с капацитет за " + guestsCount + " гости" +
                    (preferredLocation != null ? " в зона \"" + preferredLocation.getDisplayName() + "\"" : "") + ".");
        }

        // 2. Активни резервации за деня
        List<Reservation> existing = reservationRepository
                .findActiveReservationsForRestaurant(restaurantId, date);

        // 3. Само незаетите маси
        List<TableDTO> freeTables = candidates.stream()
                .filter(t -> !isTableOccupied(t.getId(), time, existing))
                .toList();

        if (freeTables.isEmpty()) {
            if (preferredLocation != null) {
                // Намираме свободни маси в другите зони
                String availableZones = restaurantServiceClient
                        .findTablesByCapacityAndLocation(restaurantId, guestsCount, null)
                        .stream()
                        .filter(t -> t.getLocation() != preferredLocation)
                        .filter(t -> !isTableOccupied(t.getId(), time, existing))
                        .map(t -> t.getLocation().getDisplayName())
                        .distinct()
                        .collect(Collectors.joining(", "));

                String suggestion = availableZones.isEmpty()
                        ? ""
                        : " Свободни маси има в: " + availableZones + ".";

                throw new TableNotAvailableException(
                        "Няма свободни маси в зона \"" + preferredLocation.getDisplayName() +
                        "\" за " + time + " на " + date + "." + suggestion);
            }
            throw new TableNotAvailableException(
                    "Всички подходящи маси са заети за " + time + " на " + date + ".");
        }

        // 4. Best-fit: избираме масата с най-малък разход (capacity - guestsCount)
        TableDTO selected = freeTables.stream()
                .min(Comparator.comparingInt(t -> t.getCapacity() - guestsCount))
                .orElseThrow();

        log.info("Избрана маса {} (капацитет: {}, зона: {}) от {} свободни",
                selected.getTableNumber(), selected.getCapacity(), selected.getLocation(), freeTables.size());
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

    // ── Налични часови слотове ────────────────────────────────────────────────

    public List<TimeSlotResponse> getAvailableTimeSlots(Long restaurantId, LocalDate date,
                                                        Integer guestsCount, TableLocation location) {
        RestaurantDTO restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
        if (restaurant.getOpeningTime() == null || restaurant.getClosingTime() == null) {
            return List.of();
        }

        List<TableDTO> candidates = restaurantServiceClient.findTablesByCapacityAndLocation(restaurantId, guestsCount, location);
        List<Reservation> existing = reservationRepository.findActiveReservationsForRestaurant(restaurantId, date);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime slot = restaurant.getOpeningTime();
        LocalTime lastSlot = restaurant.getClosingTime().minusHours(1);

        while (!slot.isAfter(lastSlot)) {
            final LocalTime current = slot;
            boolean available = candidates.stream().anyMatch(t -> !isTableOccupied(t.getId(), current, existing));
            slots.add(new TimeSlotResponse(current, available));
            slot = slot.plusMinutes(30);
        }

        return slots;
    }

    // ── Резервации на ресторант (с филтри) ───────────────────────────────────

    public List<ReservationResponse> getRestaurantReservations(Long restaurantId, LocalDate date, ReservationStatus status, Long userId, UserRole userRole) {
        if (userRole != UserRole.SYSTEM_ADMIN) {
            RestaurantDTO restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
            if (restaurant == null || !userId.equals(restaurant.getOwnerId())) {
                throw new InvalidReservationException("Нямате права да преглеждате резервациите на този ресторант.");
            }
        }
        List<Reservation> reservations;

        if (date != null && status != null) {
            reservations = reservationRepository.findByRestaurantIdAndReservationDateAndStatusOrderByReservationTimeAsc(restaurantId, date, status);
        } else if (date != null) {
            reservations = reservationRepository.findByRestaurantIdAndReservationDateOrderByReservationTimeAsc(restaurantId, date);
        } else if (status != null) {
            reservations = reservationRepository.findByRestaurantIdAndStatusOrderByReservationDateAscReservationTimeAsc(restaurantId, status);
        } else {
            reservations = reservationRepository.findByRestaurantIdOrderByReservationDateAscReservationTimeAsc(restaurantId);
        }

        return reservations.stream().map(this::mapToResponse).toList();
    }

    // ── Приключване на резервация от ресторанта ───────────────────────────────

    @Transactional
    public ReservationResponse completeReservation(Long reservationId, UserRole userRole) {
        if (userRole != UserRole.RESTAURANT_ADMIN && userRole != UserRole.SYSTEM_ADMIN) {
            throw new InvalidReservationException("Само администратори на ресторант могат да приключват резервации.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new InvalidReservationException(RESERVATION_NOT_FOUND + reservationId + NOT_FOUND_SUFFIX));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationException("Само потвърдени резервации могат да бъдат приключени.");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation = reservationRepository.save(reservation);
        log.info("Резервация {} приключена от ресторант администратор", reservationId);

        sendCompletionEmail(reservation);

        return mapToResponse(reservation);
    }

    // ── Отбелязване на неявяване ──────────────────────────────────────────────

    @Transactional
    public ReservationResponse markNoShow(Long reservationId, UserRole userRole) {
        if (userRole != UserRole.RESTAURANT_ADMIN && userRole != UserRole.SYSTEM_ADMIN) {
            throw new InvalidReservationException("Само администратори на ресторант могат да отбелязват неявяване.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new InvalidReservationException(RESERVATION_NOT_FOUND + reservationId + NOT_FOUND_SUFFIX));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationException("Само потвърдени резервации могат да бъдат отбелязани като неявяване.");
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);
        reservation = reservationRepository.save(reservation);
        log.info("Резервация {} отбелязана като NO_SHOW", reservationId);

        sendNoShowEmail(reservation);

        return mapToResponse(reservation);
    }

    // ── Авто-приключване (използва се от scheduler) ───────────────────────────

    @Transactional
    public int autoCompleteExpiredReservations() {
        LocalTime cutoffTime = LocalTime.now().minusMinutes(150);
        List<Reservation> toComplete = reservationRepository.findReservationsToAutoComplete(LocalDate.now(), cutoffTime);

        toComplete.forEach(r -> r.setStatus(ReservationStatus.COMPLETED));
        reservationRepository.saveAll(toComplete);
        toComplete.forEach(this::sendCompletionEmail);

        if (!toComplete.isEmpty()) {
            log.info("Авто-приключени {} резервации", toComplete.size());
        }
        return toComplete.size();
    }

    // ── Отказване на резервация от ресторанта ────────────────────────────────

    @Transactional
    public ReservationResponse rejectReservation(Long reservationId, Long restaurantAdminId, UserRole userRole, String reason) {
        if (userRole != UserRole.RESTAURANT_ADMIN && userRole != UserRole.SYSTEM_ADMIN) {
            throw new InvalidReservationException("Само администратори на ресторант могат да отхвърлят резервации.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new InvalidReservationException(RESERVATION_NOT_FOUND + reservationId + NOT_FOUND_SUFFIX));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationException("Само потвърдени резервации могат да бъдат отхвърлени.");
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        reservation = reservationRepository.save(reservation);
        log.info("Резервация {} отхвърлена от ресторант администратор {}", reservationId, restaurantAdminId);

        sendRejectionEmail(reservation, reason);

        return mapToResponse(reservation);
    }

    // ── Отказване на резервация от клиент ────────────────────────────────────

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, Long userId, UserRole userRole) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new InvalidReservationException(RESERVATION_NOT_FOUND + reservationId + NOT_FOUND_SUFFIX));

        boolean isSystemAdmin = userRole == UserRole.SYSTEM_ADMIN;

        if (!isSystemAdmin && !reservation.getUserId().equals(userId)) {
            throw new InvalidReservationException("Нямате право да откажете тази резервация.");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationException("Само потвърдени резервации могат да бъдат отказани.");
        }

        if (!isSystemAdmin) {
            LocalDateTime reservationDateTime = LocalDateTime.of(reservation.getReservationDate(), reservation.getReservationTime());
            if (reservationDateTime.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new InvalidReservationException("Резервацията може да се откаже само поне 2 часа преди началото й.");
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);
        log.info("Резервация {} отказана от {} ({})", reservationId, userId, userRole);

        sendCancellationEmail(reservation);

        return mapToResponse(reservation);
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

    private void sendCompletionEmail(Reservation reservation) {
        try {
            String restaurantName = getRestaurantName(reservation.getRestaurantId());
            String reviewUrl = baseUrl + "/review/" + reservation.getId();

            notificationService.sendEmail(NotificationData.builder()
                    .type(NotificationType.RESERVATION_COMPLETED)
                    .recipientEmail(reservation.getCustomerEmail())
                    .recipientName(reservation.getCustomerName())
                    .reservationId(reservation.getId())
                    .restaurantName(restaurantName)
                    .reservationDate(reservation.getReservationDate())
                    .reservationTime(reservation.getReservationTime())
                    .numberOfGuests(reservation.getNumberOfGuests())
                    .reviewUrl(reviewUrl)
                    .build());
        } catch (Exception e) {
            log.error("Грешка при изпращане на completion email за резервация {}: {}", reservation.getId(), e.getMessage());
        }
    }

    private void sendNoShowEmail(Reservation reservation) {
        try {
            String restaurantName = getRestaurantName(reservation.getRestaurantId());

            notificationService.sendEmail(NotificationData.builder()
                    .type(NotificationType.RESERVATION_NO_SHOW)
                    .recipientEmail(reservation.getCustomerEmail())
                    .recipientName(reservation.getCustomerName())
                    .reservationId(reservation.getId())
                    .restaurantName(restaurantName)
                    .reservationDate(reservation.getReservationDate())
                    .reservationTime(reservation.getReservationTime())
                    .numberOfGuests(reservation.getNumberOfGuests())
                    .build());
        } catch (Exception e) {
            log.error("Грешка при изпращане на no-show email за резервация {}: {}", reservation.getId(), e.getMessage());
        }
    }

    private void sendRejectionEmail(Reservation reservation, String reason) {
        try {
            String restaurantName = getRestaurantName(reservation.getRestaurantId());

            notificationService.sendEmail(NotificationData.builder()
                    .type(NotificationType.RESERVATION_REJECTED)
                    .recipientEmail(reservation.getCustomerEmail())
                    .recipientName(reservation.getCustomerName())
                    .reservationId(reservation.getId())
                    .restaurantName(restaurantName)
                    .reservationDate(reservation.getReservationDate())
                    .reservationTime(reservation.getReservationTime())
                    .numberOfGuests(reservation.getNumberOfGuests())
                    .specialRequests(reservation.getSpecialRequests())
                    .rejectionReason(reason)
                    .build());
        } catch (Exception e) {
            log.error("Грешка при изпращане на email за отхвърлена резервация {}: {}", reservation.getId(), e.getMessage());
        }
    }

    private void sendCancellationEmail(Reservation reservation) {
        try {
            String restaurantName = getRestaurantName(reservation.getRestaurantId());

            notificationService.sendEmail(NotificationData.builder()
                    .type(NotificationType.RESERVATION_CANCELLED)
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
            log.error("Грешка при изпращане на email за отказана резервация {}: {}", reservation.getId(), e.getMessage());
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
                .orElseThrow(() -> new InvalidReservationException(RESERVATION_NOT_FOUND + reservationId + NOT_FOUND_SUFFIX));
        return mapToResponse(reservation);
    }

    // ── Извличане на резервации на потребител ─────────────────────────────────

    public List<ReservationResponse> getMyReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservationDateDesc(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
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
