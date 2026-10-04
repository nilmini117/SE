package com.driveflow.demo_driveflow.incident;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingRepository;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.UserService;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class IncidentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IncidentService incidentService;

    @Mock
    private UserService userService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private IncidentController incidentController;

    private Customer customer;
    private Vehicle activeVehicle;
    private Booking activeBooking;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(incidentController).build();

        customer = new Customer();
        customer.setSystemId(10L);
        customer.setEmail("customer@driveflow.com");
        customer.setFirstName("John");
        customer.setLastName("Customer");

        activeVehicle = new Vehicle();
        activeVehicle.setVehicleId(101L);
        activeVehicle.setRegNo("WP CA-1020");
        activeVehicle.setModel("Prius Hybrid");
        activeVehicle.setBrand("Toyota");

        activeBooking = new Booking();
        activeBooking.setBookingId(55L);
        activeBooking.setCustomer(customer);
        activeBooking.setVehicle(activeVehicle);
        activeBooking.setStatus("CONFIRMED");
        activeBooking.setBookingDate(LocalDate.now());

        customerAuth = new UsernamePasswordAuthenticationToken(
                "customer@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("Navigating to Need to contact us form auto-binds customer's active booking vehicle")
    void showCustomerReportForm_autoBindsActiveVehicle() throws Exception {
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findActiveBookingsByCustomerId(10L)).thenReturn(List.of(activeBooking));

        mockMvc.perform(get("/incidents/report").principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(view().name("incident/incident-report"))
                .andExpect(model().attributeExists("incident"))
                .andExpect(model().attributeExists("customer"))
                .andExpect(model().attribute("activeVehicle", activeVehicle))
                .andExpect(model().attribute("activeBooking", activeBooking));

        verify(bookingRepository, times(1)).findActiveBookingsByCustomerId(10L);
    }

    @Test
    @DisplayName("GET /incidents/api/active-vehicle returns active vehicle JSON details for dynamic binding")
    void getActiveVehicle_returnsVehicleJson() throws Exception {
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findActiveBookingsByCustomerId(10L)).thenReturn(List.of(activeBooking));

        mockMvc.perform(get("/incidents/api/active-vehicle").principal(customerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveBooking").value(true))
                .andExpect(jsonPath("$.vehicleId").value(101))
                .andExpect(jsonPath("$.regNo").value("WP CA-1020"))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Prius Hybrid"))
                .andExpect(jsonPath("$.bookingId").value(55));
    }

    @Test
    @DisplayName("Submitting customer report automatically links active vehicle even without manual vehicleId param")
    void submitCustomerReport_autoLinksActiveVehicle() throws Exception {
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findActiveBookingsByCustomerId(10L)).thenReturn(List.of(activeBooking));

        mockMvc.perform(post("/incidents/report")
                .principal(customerAuth)
                .param("description", "Minor tire pressure warning during road trip")
                .param("severity", "LOW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile#incidents"));

        verify(incidentService, times(1)).logIncident(argThat(incident -> incident.getCustomer() != null &&
                incident.getCustomer().getSystemId().equals(10L) &&
                incident.getVehicle() != null &&
                incident.getVehicle().getVehicleId().equals(101L)));
    }
}
