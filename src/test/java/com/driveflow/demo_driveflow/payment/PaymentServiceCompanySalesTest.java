package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.maintenance.Maintenance;
import com.driveflow.demo_driveflow.maintenance.MaintenanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceCompanySalesTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Successfully calculate SUM(revenue) - SUM(maintenance_costs) via projection")
    void testGetCompanySalesSummary_ViaProjection() {
        // Arrange
        ReflectionTestUtils.setField(paymentService, "maintenanceRepository", maintenanceRepository);

        CompanySalesProjection mockProjection = mock(CompanySalesProjection.class);
        when(mockProjection.getTotalRevenue()).thenReturn(new BigDecimal("15000.00"));
        when(mockProjection.getTotalMaintenanceCosts()).thenReturn(new BigDecimal("3500.00"));

        when(paymentRepository.getCompanySalesSummarySql()).thenReturn(mockProjection);

        Payment payment1 = new Payment();
        payment1.setPaymentId(1L);
        payment1.setAmountPaid(new BigDecimal("10000.00"));
        payment1.setStatus("COMPLETED");

        Payment payment2 = new Payment();
        payment2.setPaymentId(2L);
        payment2.setAmountPaid(new BigDecimal("5000.00"));
        payment2.setStatus("COMPLETED");

        when(paymentRepository.findAllApprovedPayments()).thenReturn(Arrays.asList(payment1, payment2));

        Maintenance m1 = new Maintenance();
        m1.setMaintenanceId(101L);
        m1.setApproximatedCost(new BigDecimal("3500.00"));

        when(maintenanceRepository.findAll()).thenReturn(Collections.singletonList(m1));

        // Act
        CompanySalesSummaryDto summary = paymentService.getCompanySalesSummary();

        // Assert
        assertNotNull(summary);
        assertEquals(new BigDecimal("15000.00"), summary.getTotalRevenue());
        assertEquals(new BigDecimal("3500.00"), summary.getTotalMaintenanceCosts());
        // Net Income must equal SUM(revenue) - SUM(maintenance_costs) = 15000.00 - 3500.00 = 11500.00
        assertEquals(new BigDecimal("11500.00"), summary.getNetIncome());
        assertEquals(2, summary.getApprovedPaymentsCount());
        assertEquals(1, summary.getMaintenanceServicesCount());

        // Verify Corporate Bank Account Details
        assertNotNull(summary.getAccountNumber());
        assertNotNull(summary.getRoutingNumber());
        assertNotNull(summary.getSwiftCode());
        assertEquals("1000-8842-9931-5021", summary.getAccountNumber());
        assertEquals("071000288", summary.getRoutingNumber());
        assertEquals("CBCLKLX", summary.getSwiftCode());
        assertEquals("Commercial Bank of Ceylon (Corporate Banking Division)", summary.getBankName());
    }

    @Test
    @DisplayName("Fallback calculation accurately computes SUM(revenue) - SUM(maintenance_costs)")
    void testGetCompanySalesSummary_Fallback() {
        // Arrange
        ReflectionTestUtils.setField(paymentService, "maintenanceRepository", maintenanceRepository);

        // Native SQL throws or returns null
        when(paymentRepository.getCompanySalesSummarySql()).thenThrow(new RuntimeException("SQL test exception"));

        when(paymentRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("8250.50"));
        when(maintenanceRepository.calculateTotalMaintenanceCosts()).thenReturn(new BigDecimal("2100.25"));

        when(paymentRepository.findAllApprovedPayments()).thenReturn(Collections.emptyList());
        when(maintenanceRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CompanySalesSummaryDto summary = paymentService.getCompanySalesSummary();

        // Assert
        assertNotNull(summary);
        assertEquals(new BigDecimal("8250.50"), summary.getTotalRevenue());
        assertEquals(new BigDecimal("2100.25"), summary.getTotalMaintenanceCosts());
        // 8250.50 - 2100.25 = 6150.25
        assertEquals(new BigDecimal("6150.25"), summary.getNetIncome());
    }
}
