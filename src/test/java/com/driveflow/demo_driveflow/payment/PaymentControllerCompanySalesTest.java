package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentControllerCompanySalesTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private BookingService bookingService;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    @DisplayName("listPayments with tab=sales passes salesSummary and activeTab to model")
    void testListPayments_SalesTab() {
        CompanySalesSummaryDto dto = CompanySalesSummaryDto.builder()
                .totalRevenue(new BigDecimal("12000.00"))
                .totalMaintenanceCosts(new BigDecimal("4000.00"))
                .netIncome(new BigDecimal("8000.00"))
                .accountNumber("1000-8842-9931-5021")
                .routingNumber("071000288")
                .swiftCode("CBCLKLX")
                .build();

        when(paymentService.getAllPayments()).thenReturn(Collections.emptyList());
        when(paymentService.getCompanySalesSummary()).thenReturn(dto);

        Model model = new ConcurrentModel();
        String view = paymentController.listPayments("sales", model, null);

        assertEquals("payment/payment-list", view);
        assertEquals("sales", model.getAttribute("activeTab"));
        assertNotNull(model.getAttribute("salesSummary"));
        assertSame(dto, model.getAttribute("salesSummary"));
    }

    @Test
    @DisplayName("getCompanySalesApi returns corporate sales summary with dynamic net income")
    void testGetCompanySalesApi() {
        CompanySalesSummaryDto dto = CompanySalesSummaryDto.builder()
                .totalRevenue(new BigDecimal("20000.00"))
                .totalMaintenanceCosts(new BigDecimal("5000.00"))
                .netIncome(new BigDecimal("15000.00"))
                .accountNumber("1000-8842-9931-5021")
                .routingNumber("071000288")
                .swiftCode("CBCLKLX")
                .build();

        when(paymentService.getCompanySalesSummary()).thenReturn(dto);

        ResponseEntity<CompanySalesSummaryDto> response = paymentController.getCompanySalesApi();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("15000.00"), response.getBody().getNetIncome());
        assertEquals("1000-8842-9931-5021", response.getBody().getAccountNumber());
        assertEquals("071000288", response.getBody().getRoutingNumber());
        assertEquals("CBCLKLX", response.getBody().getSwiftCode());
    }
}
