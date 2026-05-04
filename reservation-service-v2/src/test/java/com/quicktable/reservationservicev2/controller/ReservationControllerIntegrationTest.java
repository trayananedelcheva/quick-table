package com.quicktable.reservationservicev2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quicktable.common.dto.TableLocation;
import com.quicktable.common.dto.UserRole;
import com.quicktable.reservationservicev2.BaseIntegrationTest;
import com.quicktable.reservationservicev2.client.RestaurantServiceClient;
import com.quicktable.reservationservicev2.client.UserServiceClient;
import com.quicktable.reservationservicev2.dto.*;
import com.quicktable.reservationservicev2.notification.NotificationService;
import com.quicktable.reservationservicev2.repository.ReservationRepository;
import com.quicktable.reservationservicev2.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ReservationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationRepository reservationRepository;

    @MockBean
    private RestaurantServiceClient restaurantServiceClient;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private NotificationService notificationService;

    private String clientToken;
    private String adminToken;
    private ReservationRequest validRequest;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();

        clientToken = "Bearer " + TestJwtUtil.generateToken(1L, "client@test.com", "CLIENT");
        adminToken = "Bearer " + TestJwtUtil.generateToken(2L, "admin@test.com", "RESTAURANT_ADMIN");

        validRequest = new ReservationRequest();
        validRequest.setRestaurantId(1L);
        validRequest.setReservationDate(LocalDate.now().plusDays(7));
        validRequest.setReservationTime(LocalTime.of(19, 0));
        validRequest.setGuestsCount(4);
        validRequest.setPreferredLocation(TableLocation.INSIDE);

        RestaurantDTO restaurant = RestaurantDTO.builder()
                .id(1L)
                .name("Тест Ресторант")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(23, 0))
                .build();
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(restaurant);

        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setEmail("client@test.com");
        user.setFirstName("Иван");
        user.setLastName("Петров");
        user.setPhoneNumber("+359888123456");
        when(userServiceClient.getUserById(eq(1L), anyString())).thenReturn(user);

        TableDTO table = new TableDTO();
        table.setId(10L);
        table.setTableNumber("В1");
        table.setCapacity(4);
        table.setLocation(TableLocation.INSIDE);
        table.setAvailable(true);
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of(table));
    }

    @Test
    @DisplayName("Успешна резервация — връща 201")
    void authenticated_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.customerName").value("Иван Петров"))
                .andExpect(jsonPath("$.numberOfGuests").value(4));
    }

    @Test
    @DisplayName("Без токен — връща 401")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Липсващо задължително поле — връща 400")
    void invalidBody_returns400() throws Exception {
        validRequest.setRestaurantId(null);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Не-CLIENT роля — връща 400")
    void nonClientRole_returns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Само клиенти могат да правят резервации."));
    }

    @Test
    @DisplayName("Резервацията се записва в базата данни")
    void reservationPersistedInDB() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        var reservations = reservationRepository.findByUserIdOrderByReservationDateDesc(1L);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getCustomerEmail()).isEqualTo("client@test.com");
        assertThat(reservations.get(0).getRestaurantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Няма свободни маси — връща 409")
    void noAvailableTable_returnsConflict() throws Exception {
        when(restaurantServiceClient.findTablesByCapacityAndLocation(1L, 4, TableLocation.INSIDE))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
