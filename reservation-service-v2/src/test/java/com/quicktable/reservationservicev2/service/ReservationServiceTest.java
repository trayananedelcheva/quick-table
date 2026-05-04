package com.quicktable.reservationservicev2.service;

import com.quicktable.common.dto.ReservationStatus;
import com.quicktable.common.dto.TableLocation;
import com.quicktable.common.dto.UserRole;
import com.quicktable.reservationservicev2.client.RestaurantServiceClient;
import com.quicktable.reservationservicev2.client.UserServiceClient;
import com.quicktable.reservationservicev2.dto.*;
import com.quicktable.reservationservicev2.entity.Reservation;
import com.quicktable.reservationservicev2.exception.InvalidReservationException;
import com.quicktable.reservationservicev2.exception.TableNotAvailableException;
import com.quicktable.reservationservicev2.notification.NotificationService;
import com.quicktable.reservationservicev2.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReservationService reservationService;

    private ReservationRequest validRequest;
    private UserDTO testUser;
    private RestaurantDTO testRestaurant;
    private TableDTO testTable;

    @BeforeEach
    void setUp() {
        validRequest = new ReservationRequest();
        validRequest.setRestaurantId(1L);
        validRequest.setReservationDate(LocalDate.now().plusDays(7));
        validRequest.setReservationTime(LocalTime.of(19, 0));
        validRequest.setGuestsCount(4);
        validRequest.setPreferredLocation(TableLocation.INSIDE);

        testUser = new UserDTO();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");
        testUser.setFirstName("Иван");
        testUser.setLastName("Петров");
        testUser.setPhoneNumber("+359888123456");

        testRestaurant = RestaurantDTO.builder()
                .id(1L)
                .name("Тест Ресторант")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(23, 0))
                .build();

        testTable = new TableDTO();
        testTable.setId(10L);
        testTable.setTableNumber("В1");
        testTable.setCapacity(4);
        testTable.setLocation(TableLocation.INSIDE);
        testTable.setAvailable(true);
    }

    // ── Валидация на роля ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Не-CLIENT роля не може да прави резервация")
    void nonClientRole_throwsException() {
        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.RESTAURANT_ADMIN, "token", validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("Само клиенти");
    }

    // ── Валидация на време (1 час напред) ─────────────────────────────────────

    @Test
    @DisplayName("Резервация по-малко от 1 час напред — отказ")
    void lessThanOneHourInAdvance_throwsException() {
        validRequest.setReservationDate(LocalDate.now());
        validRequest.setReservationTime(LocalTime.now().plusMinutes(30));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("поне 1 час");
    }

    // ── Валидация на time slot ─────────────────────────────────────────────────

    @Test
    @DisplayName("Час не на кръгъл 30 мин — отказ")
    void invalidTimeSlot_throwsException() {
        validRequest.setReservationTime(LocalTime.of(10, 15));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("начало на час или в половината");
    }

    @Test
    @DisplayName("Час преди отваряне на ресторанта — отказ")
    void beforeRestaurantOpening_throwsException() {
        validRequest.setReservationTime(LocalTime.of(8, 0));
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(testRestaurant);

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("отваря в");
    }

    @Test
    @DisplayName("Час след последния допустим слот — отказ")
    void afterLastSlot_throwsException() {
        validRequest.setReservationTime(LocalTime.of(22, 30));
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(testRestaurant);

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("Последният възможен час");
    }

    // ── Валидация на маси ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Няма маса с достатъчен капацитет — отказ")
    void noTablesWithCapacity_throwsTableNotAvailable() {
        setupValidTimeChecks();
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class)
                .hasMessageContaining("Няма маса с капацитет");
    }

    @Test
    @DisplayName("Всички маси заети в 2h прозорец — отказ")
    void allTablesOccupied_throwsTableNotAvailable() {
        setupValidTimeChecks();
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(testTable));

        Reservation existingReservation = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(18, 0))
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(1L, validRequest.getReservationDate()))
                .thenReturn(List.of(existingReservation));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class)
                .hasMessageContaining("заети");
    }

    @Test
    @DisplayName("Предишна резервация свършва точно когато новата започва — няма конфликт")
    void overlapEdgeCase_noConflict() {
        setupValidTimeChecks();
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(testTable));

        // Съществуваща резервация: 17:00-19:00, нова: 19:00-21:00 → няма overlap
        Reservation existingReservation = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(17, 0))
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(1L, validRequest.getReservationDate()))
                .thenReturn(List.of(existingReservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        ReservationResponse response = reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTableId()).isEqualTo(10L);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Успешна резервация — записва правилните полета")
    void success_savesCorrectFields() {
        setupValidTimeChecks();
        setupValidTableSearch();

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(captor.capture()))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest);

        Reservation saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getRestaurantId()).isEqualTo(1L);
        assertThat(saved.getTableId()).isEqualTo(10L);
        assertThat(saved.getNumberOfGuests()).isEqualTo(4);
        assertThat(saved.getCustomerName()).isEqualTo("Иван Петров");
        assertThat(saved.getCustomerEmail()).isEqualTo("test@email.com");
        assertThat(saved.getCustomerPhone()).isEqualTo("+359888123456");
    }

    @Test
    @DisplayName("Успешна резервация — статус е CONFIRMED")
    void success_statusIsConfirmed() {
        setupValidTimeChecks();
        setupValidTableSearch();
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        ReservationResponse response = reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Успешна резервация — изпраща email")
    void success_sendsEmail() {
        setupValidTimeChecks();
        setupValidTableSearch();
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest);

        verify(notificationService).sendEmail(any());
    }

    @Test
    @DisplayName("Грешка при email — резервацията не се чупи")
    void emailFailure_doesNotThrow() {
        setupValidTimeChecks();
        setupValidTableSearch();
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });
        doThrow(new RuntimeException("SMTP error")).when(notificationService).sendEmail(any());

        ReservationResponse response = reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setupValidTimeChecks() {
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(testRestaurant);
        when(userServiceClient.getUserById(1L, "token")).thenReturn(testUser);
    }

    private void setupValidTableSearch() {
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(testTable));
        when(reservationRepository.findActiveReservationsForRestaurant(1L, validRequest.getReservationDate()))
                .thenReturn(List.of());
    }
}
