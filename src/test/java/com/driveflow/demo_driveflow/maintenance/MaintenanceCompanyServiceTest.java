package com.driveflow.demo_driveflow.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MaintenanceCompanyServiceTest {

    @Mock
    private MaintenanceCompanyRepository maintenanceCompanyRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private MaintenanceCompanyServiceImpl maintenanceCompanyService;

    private MaintenanceCompany validCompany;

    @BeforeEach
    void setUp() {
        validCompany = new MaintenanceCompany();
        validCompany.setCompanyId(1L);
        validCompany.setCompanyName("AutoCare Precision Services");
        validCompany.setEmail("autocare@precisionfleet.com");
        validCompany.setContactNumber("0112894567");
        validCompany.setAddress("45 Station Road, Colombo 03");
        validCompany.setSpeciality("Engine & Transmission Overhaul");
    }

    @Test
    @DisplayName("Verify creating maintenance company succeeds with valid 10-digit contact number")
    void createCompany_shouldSucceed_whenContactNumberIs10Digits() {
        when(maintenanceCompanyRepository.save(any(MaintenanceCompany.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceCompany created = maintenanceCompanyService.createCompany(validCompany);

        assertNotNull(created);
        assertEquals("0112894567", created.getContactNumber());
        verify(maintenanceCompanyRepository, times(1)).save(validCompany);
    }

    @Test
    @DisplayName("Verify creating maintenance company fails with 9-digit contact number")
    void createCompany_shouldFail_whenContactNumberIs9Digits() {
        validCompany.setContactNumber("011289456"); // 9 digits

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceCompanyService.createCompany(validCompany);
        });

        assertTrue(ex.getMessage().contains("exactly 10 digits long"));
        verify(maintenanceCompanyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify creating maintenance company fails with 11-digit contact number")
    void createCompany_shouldFail_whenContactNumberIs11Digits() {
        validCompany.setContactNumber("01128945678"); // 11 digits

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceCompanyService.createCompany(validCompany);
        });

        assertTrue(ex.getMessage().contains("exactly 10 digits long"));
        verify(maintenanceCompanyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify creating maintenance company fails when contact number contains letters or dashes")
    void createCompany_shouldFail_whenContactNumberContainsNonDigits() {
        validCompany.setContactNumber("011-2894567"); // contains hyphen

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceCompanyService.createCompany(validCompany);
        });

        assertTrue(ex.getMessage().contains("exactly 10 digits long"));
        verify(maintenanceCompanyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify updating maintenance company fails with 9-digit contact number")
    void updateCompany_shouldFail_whenContactNumberIs9Digits() {
        MaintenanceCompany updatePayload = new MaintenanceCompany();
        updatePayload.setCompanyName("AutoCare Updated");
        updatePayload.setEmail("update@precisionfleet.com");
        updatePayload.setContactNumber("077123456"); // 9 digits

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceCompanyService.updateCompany(1L, updatePayload);
        });

        assertTrue(ex.getMessage().contains("exactly 10 digits long"));
        verify(maintenanceCompanyRepository, never()).save(any());
    }
}
