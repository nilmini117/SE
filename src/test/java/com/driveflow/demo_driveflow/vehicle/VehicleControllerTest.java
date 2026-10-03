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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class VehicleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private com.driveflow.demo_driveflow.feedback.FeedbackService feedbackService;

    @InjectMocks
    private VehicleController vehicleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleController).build();
    }

    @Test
    @DisplayName("Verify /vehicles provides dynamic brandCounts model attribute for rendering dynamic brand cards")
    void listVehicles_shouldProvideDynamicBrandCounts() throws Exception {
        Map<String, Long> dynamicCounts = new LinkedHashMap<>();
        dynamicCounts.put("Toyota", 4L);
        dynamicCounts.put("Tesla", 2L);
        dynamicCounts.put("Suzuki", 0L);
        dynamicCounts.put("Honda", 0L);
        dynamicCounts.put("Benz", 0L);

        when(vehicleService.getAllBrands()).thenReturn(List.of("Toyota", "Suzuki", "Honda", "Tesla", "Benz"));
        when(vehicleService.searchVehicles(any(), eq("AVAILABLE"))).thenReturn(List.of());
        when(vehicleService.getBrandVehicleCounts()).thenReturn(dynamicCounts);
        when(feedbackService.getPubliclyVisibleFeedback()).thenReturn(List.of());

        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/vehicle-list"))
                .andExpect(model().attributeExists("brandCounts"))
                .andExpect(model().attribute("brandCounts", hasEntry("Toyota", 4L)))
                .andExpect(model().attribute("brandCounts", hasEntry("Tesla", 2L)));
    }

    @Test
    @DisplayName("Verify /vehicles/api/brand-counts endpoint returns dynamic counts JSON for frontend state integration")
    void getBrandCountsApi_shouldReturnJsonMap() throws Exception {
        Map<String, Long> dynamicCounts = new LinkedHashMap<>();
        dynamicCounts.put("Toyota", 4L);
        dynamicCounts.put("Tesla", 2L);

        when(vehicleService.getBrandVehicleCounts()).thenReturn(dynamicCounts);

        mockMvc.perform(get("/vehicles/api/brand-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Toyota").value(4))
                .andExpect(jsonPath("$.Tesla").value(2));
    }

    @Test
    @DisplayName("Verify /vehicles/api/by-brand returns vehicle array for client-side filter and reduce iterations")
    void getVehiclesByBrandApi_shouldReturnVehiclesList() throws Exception {
        Vehicle v1 = new Vehicle(1L, "WP CA-1020", "Toyota Prius", 1, "White", 35000, "AVAILABLE", null);
        Vehicle v2 = new Vehicle(2L, "WP TM-3001", "Tesla Model 3", 1, "Blue", 8000, "AVAILABLE", null);

        when(vehicleService.getAvailableVehiclesByBrand(null)).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/vehicles/api/by-brand").param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].brand").value("Toyota"))
                .andExpect(jsonPath("$[1].brand").value("Tesla"));
    }
}
