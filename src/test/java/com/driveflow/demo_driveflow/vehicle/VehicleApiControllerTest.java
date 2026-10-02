package com.driveflow.demo_driveflow.vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class VehicleApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleApiController vehicleApiController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleApiController).build();
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/{id} calls service removeVehicle and returns success response")
    void deleteVehicle_shouldCallServiceAndReturnOk() throws Exception {
        doNothing().when(vehicleService).removeVehicle(10L);

        mockMvc.perform(delete("/api/vehicles/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedId").value(10));

        verify(vehicleService, times(1)).removeVehicle(10L);
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/{id} returns 400 when service throws exception")
    void deleteVehicle_shouldHandleErrorGracefully() throws Exception {
        doThrow(new RuntimeException("Vehicle not found")).when(vehicleService).removeVehicle(999L);

        mockMvc.perform(delete("/api/vehicles/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Verify DELETE /api/vehicles/reg/{regNo} finds vehicle by regNo and calls removeVehicle")
    void deleteVehicleByRegNo_shouldFindAndRemove() throws Exception {
        Vehicle vehicle = new Vehicle(15L, "WP CA-5555", "Toyota Prius", 1, "White", 20000, "AVAILABLE", null);
        when(vehicleService.getAllVehicles()).thenReturn(List.of(vehicle));
        doNothing().when(vehicleService).removeVehicle(15L);

        mockMvc.perform(delete("/api/vehicles/reg/WP CA-5555"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedId").value(15));

        verify(vehicleService, times(1)).removeVehicle(15L);
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
