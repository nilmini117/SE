package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.payment.CompanySalesSummaryDto;
import com.driveflow.demo_driveflow.payment.PaymentService;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MaintenanceControllerTest {

    @Mock
    private MaintenanceService maintenanceService;

    @Mock
    private MaintenanceCompanyService maintenanceCompanyService;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private MaintenanceController maintenanceController;

    private Vehicle sampleVehicle;
    private MaintenanceCompany sampleCompany;

    @BeforeEach
    void setUp() {
        sampleVehicle = new Vehicle();
        sampleVehicle.setVehicleId(1L);
        sampleVehicle.setModel("Toyota Axio");
        sampleVehicle.setRegNo("WP CA-2020");

        sampleCompany = new MaintenanceCompany();
        sampleCompany.setCompanyId(10L);
        sampleCompany.setCompanyName("AutoCare Precision Services");
        sampleCompany.setEmail("autocare@precisionfleet.com");
        sampleCompany.setContactNumber("0112894567");
    }

    @Test
    @DisplayName("Verify scheduleService blocks submission and returns 400 Bad Request when cost >= net income")
    void scheduleService_shouldReturnBadRequest_whenCostExceedsNetIncome() {
        CompanySalesSummaryDto sales = CompanySalesSummaryDto.builder()
                .netIncome(new BigDecimal("15000.00"))
                .build();
        when(paymentService.getCompanySalesSummary()).thenReturn(sales);

        Maintenance record = new Maintenance();
        record.setServiceDate(LocalDate.now());
        BigDecimal enteredCost = new BigDecimal("25000.00"); // exceeds 15,000.00

        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = maintenanceController.scheduleService(record, 1L, 10L, enteredCost, response, model, redirectAttributes);

        assertEquals("maintenance/schedule-form", view);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Must set 400 Bad Request");
        assertNotNull(model.getAttribute("errorMessage"));
        assertTrue(model.getAttribute("errorMessage").toString().contains("strictly less than"));
        verify(maintenanceService, never()).scheduleService(any());
    }

    @Test
    @DisplayName("Verify scheduleService blocks submission and returns 400 when cost equals net income")
    void scheduleService_shouldReturnBadRequest_whenCostEqualsNetIncome() {
        CompanySalesSummaryDto sales = CompanySalesSummaryDto.builder()
                .netIncome(new BigDecimal("20000.00"))
                .build();
        when(paymentService.getCompanySalesSummary()).thenReturn(sales);

        Maintenance record = new Maintenance();
        record.setServiceDate(LocalDate.now());
        BigDecimal enteredCost = new BigDecimal("20000.00"); // equal to net income

        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = maintenanceController.scheduleService(record, 1L, 10L, enteredCost, response, model, redirectAttributes);

        assertEquals("maintenance/schedule-form", view);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Must set 400 Bad Request");
        verify(maintenanceService, never()).scheduleService(any());
    }

    @Test
    @DisplayName("Verify scheduleService succeeds when cost is strictly less than net income")
    void scheduleService_shouldSucceed_whenCostIsLessThanNetIncome() {
        CompanySalesSummaryDto sales = CompanySalesSummaryDto.builder()
                .netIncome(new BigDecimal("50000.00"))
                .build();
        when(paymentService.getCompanySalesSummary()).thenReturn(sales);
        when(vehicleService.getVehicleById(1L)).thenReturn(sampleVehicle);
        when(maintenanceCompanyService.getCompanyById(10L)).thenReturn(sampleCompany);

        Maintenance saved = new Maintenance();
        saved.setMaintenanceId(101L);
        saved.setVehicle(sampleVehicle);
        saved.setMaintenanceCompany(sampleCompany);
        when(maintenanceService.scheduleService(any(Maintenance.class))).thenReturn(saved);

        Maintenance record = new Maintenance();
        record.setServiceDate(LocalDate.now());
        BigDecimal enteredCost = new BigDecimal("12000.00"); // strictly less than 50,000.00

        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = maintenanceController.scheduleService(record, 1L, 10L, enteredCost, response, model, redirectAttributes);

        assertEquals("redirect:/maintenance", view);
        verify(maintenanceService, times(1)).scheduleService(any(Maintenance.class));
    }

    @Test
    @DisplayName("Verify createCompany returns 400 Bad Request when contact number has 9 digits")
    void createCompany_shouldReturnBadRequest_whenContactNumberIs9Digits() {
        MaintenanceCompany company = new MaintenanceCompany();
        company.setCompanyName("Apex Mechanics");
        company.setEmail("apex@mechanics.com");
        company.setContactNumber("011289456"); // 9 digits

        BindingResult bindingResult = new BeanPropertyBindingResult(company, "company");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = maintenanceController.createCompany(company, bindingResult, response, model, redirectAttributes);

        assertEquals("maintenance/company-form", view);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(bindingResult.hasFieldErrors("contactNumber"));
        verify(maintenanceCompanyService, never()).createCompany(any());
    }

    @Test
    @DisplayName("Verify createCompany succeeds when contact number has exactly 10 digits")
    void createCompany_shouldSucceed_whenContactNumberIs10Digits() {
        MaintenanceCompany company = new MaintenanceCompany();
        company.setCompanyName("Apex Mechanics");
        company.setEmail("apex@mechanics.com");
        company.setContactNumber("0112894567"); // 10 digits

        BindingResult bindingResult = new BeanPropertyBindingResult(company, "company");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = maintenanceController.createCompany(company, bindingResult, response, model, redirectAttributes);

        assertEquals("redirect:/maintenance/companies", view);
        verify(maintenanceCompanyService, times(1)).createCompany(company);
    }

    @Test
    @DisplayName("Verify updateCompany returns 400 Bad Request when contact number has 9 digits")
    void updateCompany_shouldReturnBadRequest_whenContactNumberIs9Digits() {
        MaintenanceCompany company = new MaintenanceCompany();
        company.setCompanyName("Apex Mechanics");
        company.setEmail("apex@mechanics.com");
        company.setContactNumber("077123456"); // 9 digits

        BindingResult bindingResult = new BeanPropertyBindingResult(company, "company");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = maintenanceController.updateCompany(10L, company, bindingResult, response, model, redirectAttributes);

        assertEquals("maintenance/company-form", view);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(bindingResult.hasFieldErrors("contactNumber"));
        verify(maintenanceCompanyService, never()).updateCompany(any(), any());
    }
}
