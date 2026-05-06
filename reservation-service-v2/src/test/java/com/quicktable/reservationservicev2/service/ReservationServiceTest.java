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
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, null))
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
                .hasMessageContaining("Няма свободни маси в зона");
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

    // ── Best-fit избор на маса ────────────────────────────────────────────────

    @Test
    @DisplayName("Best-fit: избира масата с най-малък разход (не случайна)")
    void bestFit_selectsSmallestFittingTable() {
        // Група от 3, маси с капацитет 4, 6, 8 → трябва да се избере 4
        TableDTO table4 = tableWithId(10L, 4, TableLocation.INSIDE);
        TableDTO table6 = tableWithId(11L, 6, TableLocation.INSIDE);
        TableDTO table8 = tableWithId(12L, 8, TableLocation.INSIDE);

        validRequest.setGuestsCount(3);
        setupValidTimeChecks();
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 3, TableLocation.INSIDE))
                .thenReturn(List.of(table4, table6, table8));
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        saveAndReturn(captor);

        assertThat(captor.getValue().getTableId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Best-fit: при точно съвпадение избира точната маса")
    void bestFit_exactCapacityMatch() {
        TableDTO tableExact = tableWithId(10L, 4, TableLocation.INSIDE);
        TableDTO tableLarger = tableWithId(11L, 6, TableLocation.INSIDE);

        setupValidTimeChecks();
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(tableExact, tableLarger));
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        saveAndReturn(captor);

        assertThat(captor.getValue().getTableId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Best-fit: ако точната маса е заета, взима следващата най-подходяща")
    void bestFit_exactTableOccupied_takesNextBestFit() {
        TableDTO table4 = tableWithId(10L, 4, TableLocation.INSIDE);
        TableDTO table6 = tableWithId(11L, 6, TableLocation.INSIDE);

        setupValidTimeChecks();
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(table4, table6));

        // Масата за 4 е заета в 18:30 → блокира 19:00
        Reservation occupied = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(18, 30))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(occupied));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        saveAndReturn(captor);

        assertThat(captor.getValue().getTableId()).isEqualTo(11L);
    }

    // ── Зона: съобщения за алтернативи ───────────────────────────────────────

    @Test
    @DisplayName("Заета зона — съобщението предлага свободна алтернативна зона")
    void zoneFull_suggestsOtherAvailableZone() {
        setupValidTimeChecks();

        TableDTO insideTable = tableWithId(10L, 4, TableLocation.INSIDE);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(insideTable));

        Reservation occupied = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(19, 0))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(occupied));

        // Лятна градина има свободна маса
        TableDTO gardenTable = tableWithId(20L, 4, TableLocation.SUMMER_GARDEN);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, null))
                .thenReturn(List.of(insideTable, gardenTable));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class)
                .hasMessageContaining("Вътре")
                .hasMessageContaining("Свободни маси има в")
                .hasMessageContaining("Лятна градина");
    }

    @Test
    @DisplayName("Заета зона — без алтернативи, без допълнително предложение")
    void zoneFull_noOtherZones_noSuggestion() {
        setupValidTimeChecks();

        TableDTO insideTable = tableWithId(10L, 4, TableLocation.INSIDE);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(insideTable));

        Reservation occupied = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(19, 0))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(occupied));

        // Само INSIDE съществува, и тя е заета
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, null))
                .thenReturn(List.of(insideTable));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class)
                .hasMessageContaining("Вътре")
                .hasMessageNotContaining("Свободни маси има в");
    }

    @Test
    @DisplayName("Без зона — всички маси заети — generic съобщение")
    void noZonePreference_allTablesFull_genericMessage() {
        validRequest.setPreferredLocation(null);
        setupValidTimeChecks();

        TableDTO table = tableWithId(10L, 4, TableLocation.INSIDE);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, null))
                .thenReturn(List.of(table));

        Reservation occupied = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(19, 0))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(occupied));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class)
                .hasMessageContaining("Всички подходящи маси са заети");
    }

    // ── Overlap на времевия прозорец ──────────────────────────────────────────

    @Test
    @DisplayName("Overlap: съществуваща 18:00 (свършва 20:00) блокира нова в 19:00")
    void overlap_eighteenToTwenty_blocksNineteenOClock() {
        setupValidTimeChecks();

        TableDTO table = tableWithId(10L, 4, TableLocation.INSIDE);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(table));

        Reservation occupied = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(18, 0))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(occupied));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class);
    }

    @Test
    @DisplayName("Overlap: съществуваща 20:00 (свършва 22:00) блокира нова в 19:00")
    void overlap_twentyToTwentyTwo_blocksNineteenOClock() {
        setupValidTimeChecks();

        TableDTO table = tableWithId(10L, 4, TableLocation.INSIDE);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(table));

        Reservation occupied = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(20, 0))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(occupied));

        assertThatThrownBy(() ->
                reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest))
                .isInstanceOf(TableNotAvailableException.class);
    }

    @Test
    @DisplayName("Overlap edge case: съществуваща 17:00 (свършва точно в 19:00) НЕ блокира нова в 19:00")
    void overlap_edgeCase_seventeenToNineteen_doesNotBlockNineteen() {
        setupValidTimeChecks();

        TableDTO table = tableWithId(10L, 4, TableLocation.INSIDE);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(table));

        Reservation finished = Reservation.builder()
                .tableId(10L)
                .reservationTime(LocalTime.of(17, 0))
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findActiveReservationsForRestaurant(eq(1L), any()))
                .thenReturn(List.of(finished));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        ReservationResponse response = saveAndReturn(captor);

        assertThat(response).isNotNull();
        assertThat(captor.getValue().getTableId()).isEqualTo(10L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setupValidTimeChecks() {
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(testRestaurant);
        when(userServiceClient.getUserById(1L, "token")).thenReturn(testUser);
        when(reservationRepository.findConfirmedReservationsByUserAndDate(eq(1L), any()))
                .thenReturn(List.of());
    }

    private void setupValidTableSearch() {
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(testTable));
        when(reservationRepository.findActiveReservationsForRestaurant(1L, validRequest.getReservationDate()))
                .thenReturn(List.of());
    }

    private ReservationResponse saveAndReturn(ArgumentCaptor<Reservation> captor) {
        when(reservationRepository.save(captor.capture()))
                .thenAnswer(inv -> { Reservation r = inv.getArgument(0); r.setId(1L); return r; });
        return reservationService.createReservation(1L, UserRole.CLIENT, "token", validRequest);
    }

    private TableDTO tableWithId(Long id, int capacity, TableLocation location) {
        TableDTO t = new TableDTO();
        t.setId(id);
        t.setTableNumber("T" + id);
        t.setCapacity(capacity);
        t.setLocation(location);
        t.setAvailable(true);
        return t;
    }
}
