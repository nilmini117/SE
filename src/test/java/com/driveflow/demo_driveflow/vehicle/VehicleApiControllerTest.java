package com.driveflow.demo_driveflow.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class VehicleApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleApiController vehicleApiController;

    private Authentication staffAuth;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleApiController).build();

        staffAuth = new UsernamePasswordAuthenticationToken(
                "staff@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        customerAuth = new UsernamePasswordAuthenticationToken(
                "customer@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/{id} with STAFF credentials calls service removeVehicle and returns success response")
    void deleteVehicle_shouldCallServiceAndReturnOk() throws Exception {
        doNothing().when(vehicleService).removeVehicle(10L);

        mockMvc.perform(delete("/api/vehicles/10").principal(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedId").value(10));

        verify(vehicleService, times(1)).removeVehicle(10L);
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/{id} with CUSTOMER credentials returns 403 Forbidden")
    void deleteVehicle_withCustomerAuth_returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/vehicles/10").principal(customerAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").exists());

        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/{id} unauthenticated returns 403 Forbidden")
    void deleteVehicle_unauthenticated_returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/vehicles/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Verify PUT /api/vehicles/{id} with CUSTOMER credentials returns 403 Forbidden")
    void updateVehicle_withCustomerAuth_returns403Forbidden() throws Exception {
        String payload = """
                {
                    "brand": "Toyota",
                    "model": "Illegal Model Update"
                }
                """;

        mockMvc.perform(put("/api/vehicles/10")
                        .principal(customerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).updateVehicle(any(), any());
    }

    @Test
    @DisplayName("Verify PUT /api/vehicles/{id} unauthenticated returns 403 Forbidden")
    void updateVehicle_unauthenticated_returns403Forbidden() throws Exception {
        mockMvc.perform(put("/api/vehicles/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(vehicleService, never()).updateVehicle(any(), any());
    }

    @Test
    @DisplayName("Verify PUT /api/vehicles/{id} with STAFF credentials calls service updateVehicle and returns 200 OK")
    void updateVehicle_withStaffAuth_callsServiceAndReturnsOk() throws Exception {
        Vehicle updatedVehicle = new Vehicle(10L, "WP CA-1020", "Toyota Prius 2024", 1, "Pearl White", 35000, "AVAILABLE", null);
        when(vehicleService.updateVehicle(eq(10L), any(Vehicle.class))).thenReturn(updatedVehicle);

        String payload = """
                {
                    "brand": "Toyota",
                    "model": "Toyota Prius 2024",
                    "color": "Pearl White"
                }
                """;

        mockMvc.perform(put("/api/vehicles/10")
                        .principal(staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.vehicle.model").value("Toyota Prius 2024"));

        verify(vehicleService, times(1)).updateVehicle(eq(10L), any(Vehicle.class));
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/{id} returns 400 when service throws exception for STAFF")
    void deleteVehicle_shouldHandleErrorGracefully() throws Exception {
        doThrow(new RuntimeException("Vehicle not found")).when(vehicleService).removeVehicle(999L);

        mockMvc.perform(delete("/api/vehicles/999").principal(staffAuth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/reg/{regNo} finds vehicle by regNo and calls removeVehicle for STAFF")
    void deleteVehicleByRegNo_shouldFindAndRemove() throws Exception {
        Vehicle vehicle = new Vehicle(15L, "WP CA-5555", "Toyota Prius", 1, "White", 20000, "AVAILABLE", null);
        when(vehicleService.getAllVehicles()).thenReturn(List.of(vehicle));
        doNothing().when(vehicleService).removeVehicle(15L);

        mockMvc.perform(delete("/api/vehicles/reg/WP CA-5555").principal(staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedId").value(15));

        verify(vehicleService, times(1)).removeVehicle(15L);
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/reg/{regNo} with CUSTOMER credentials returns 403 Forbidden")
    void deleteVehicleByRegNo_withCustomerAuth_returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/vehicles/reg/WP CA-5555").principal(customerAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Verify GET /api/vehicles returns active vehicles")
    void getActiveVehicles_shouldReturnList() throws Exception {
        Vehicle v1 = new Vehicle(1L, "WP CA-1020", "Toyota Prius", 1, "White", 35000, "AVAILABLE", null);
        when(vehicleService.getVehiclesByBrand(null)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vehicleId").value(1))
                .andExpect(jsonPath("$[0].model").value("Toyota Prius"))
                .andExpect(jsonPath("$[0].brand").value("Toyota"));
    }
}
