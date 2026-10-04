package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class VehicleSecurityTest {

    private MockMvc apiMockMvc;
    private MockMvc webMockMvc;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private com.driveflow.demo_driveflow.feedback.FeedbackService feedbackService;

    @InjectMocks
    private VehicleApiController vehicleApiController;

    @InjectMocks
    private VehicleController vehicleController;

    private Authentication staffAuth;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        apiMockMvc = MockMvcBuilders.standaloneSetup(vehicleApiController).build();
        webMockMvc = MockMvcBuilders.standaloneSetup(vehicleController).build();

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
    @DisplayName("Definition of Done: Proving that a DELETE /api/vehicles/{id} request sent with a CUSTOMER JWT returns a 403 Forbidden response")
    void deleteVehicle_withCustomerJwt_returns403Forbidden() throws Exception {
        // Customer JWT/session attempting to delete a vehicle from the catalog
        apiMockMvc.perform(delete("/api/vehicles/10")
                        .principal(customerAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied: Only staff members are permitted to delete vehicles."));

        // Verify service level delete was NEVER invoked
        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Security: Unauthenticated DELETE /api/vehicles/{id} request returns a 403 Forbidden response")
    void deleteVehicle_unauthenticated_returns403Forbidden() throws Exception {
        // Anonymous/unauthenticated request attempting to delete a vehicle
        apiMockMvc.perform(delete("/api/vehicles/10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Security: Proving that a PUT /api/vehicles/{id} request sent with a CUSTOMER JWT returns a 403 Forbidden response")
    void updateVehicle_withCustomerJwt_returns403Forbidden() throws Exception {
        String payload = """
                {
                    "brand": "Toyota",
                    "model": "Unauthorized Update by Customer"
                }
                """;

        apiMockMvc.perform(put("/api/vehicles/10")
                        .principal(customerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied: Only staff members are permitted to modify vehicles."));

        verify(vehicleService, never()).updateVehicle(any(), any());
    }

    @Test
    @DisplayName("Security: Proving that an unauthenticated PUT /api/vehicles/{id} request returns a 403 Forbidden response")
    void updateVehicle_unauthenticated_returns403Forbidden() throws Exception {
        apiMockMvc.perform(put("/api/vehicles/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).updateVehicle(any(), any());
    }

    @Test
    @DisplayName("Security: Proving that a DELETE /vehicles/{id} request sent with a CUSTOMER returns a 403 Forbidden response")
    void deleteVehicleRoot_withCustomer_returns403Forbidden() throws Exception {
        webMockMvc.perform(delete("/vehicles/10")
                        .principal(customerAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Security: Proving that a DELETE /vehicles/api/{id} request sent with a CUSTOMER returns a 403 Forbidden response")
    void deleteVehicleApiEndpoint_withCustomer_returns403Forbidden() throws Exception {
        webMockMvc.perform(delete("/vehicles/api/10")
                        .principal(customerAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).removeVehicle(any());
    }

    @Test
    @DisplayName("Verification: Proving that a DELETE /api/vehicles/{id} request sent with a STAFF JWT succeeds with 200 OK")
    void deleteVehicle_withStaffJwt_succeedsWith200Ok() throws Exception {
        doNothing().when(vehicleService).removeVehicle(10L);

        apiMockMvc.perform(delete("/api/vehicles/10")
                        .principal(staffAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedId").value(10));

        verify(vehicleService, times(1)).removeVehicle(10L);
    }

    @Test
    @DisplayName("Verification: Proving that a PUT /api/vehicles/{id} request sent with a STAFF JWT succeeds with 200 OK")
    void updateVehicle_withStaffJwt_succeedsWith200Ok() throws Exception {
        Vehicle updated = new Vehicle(10L, "WP CA-1020", "Toyota Camry Hybrid", 1, "Midnight Black", 22000, "AVAILABLE", null);
        when(vehicleService.updateVehicle(eq(10L), any(Vehicle.class))).thenReturn(updated);

        String payload = """
                {
                    "brand": "Toyota",
                    "model": "Toyota Camry Hybrid",
                    "color": "Midnight Black"
                }
                """;

        apiMockMvc.perform(put("/api/vehicles/10")
                        .principal(staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.vehicle.model").value("Toyota Camry Hybrid"));

        verify(vehicleService, times(1)).updateVehicle(eq(10L), any(Vehicle.class));
    }
}
