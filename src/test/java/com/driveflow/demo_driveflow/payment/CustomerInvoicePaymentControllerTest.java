package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.users.UserService;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerInvoicePaymentControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomerInvoicePaymentController controller;

    private Customer customer;
    private Booking confirmedBooking;
    private Booking pendingBooking;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setSystemId(1L);
        customer.setEmail("customer@driveflow.com");
        customer.setFirstName("Jane");
        customer.setLastName("Doe");

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(10L);
        vehicle.setModel("Toyota Prius 2024");
        vehicle.setRegNo("WP CA-1020");

        confirmedBooking = new Booking();
        confirmedBooking.setBookingId(100L);
        confirmedBooking.setCustomer(customer);
        confirmedBooking.setVehicle(vehicle);
        confirmedBooking.setStatus("CONFIRMED");
        confirmedBooking.setBookingDate(LocalDate.now());
        confirmedBooking.setEndDate(LocalDate.now().plusDays(3));
        confirmedBooking.setChargedRate(new BigDecimal("150.00"));

        pendingBooking = new Booking();
        pendingBooking.setBookingId(101L);
        pendingBooking.setCustomer(customer);
        pendingBooking.setVehicle(vehicle);
        pendingBooking.setStatus("PENDING");
        pendingBooking.setChargedRate(new BigDecimal("200.00"));

        invoice = new Invoice();
        invoice.setInvoiceId(50L);
        invoice.setBooking(confirmedBooking);
        invoice.setTotalAmt(new BigDecimal("150.00"));
        invoice.setStatus("UNPAID");
    }

    @Test
    @DisplayName("viewInvoicesAndPayments strictly filters confirmed bookings and sets activeTab")
    void testViewInvoicesAndPayments_ConfirmedOnly() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));

        // Return both confirmed and pending bookings
        when(bookingService.getBookingsByCustomer(customer))
                .thenReturn(Arrays.asList(confirmedBooking, pendingBooking));
        when(invoiceRepository.findByBooking(confirmedBooking)).thenReturn(Optional.of(invoice));
        when(paymentService.getPaymentsByCustomer(customer)).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = controller.viewInvoicesAndPayments(null, model, authentication);

        assertEquals("customer/invoices-payments", view);
        assertEquals("invoices-payments", model.getAttribute("activeTab"));

        @SuppressWarnings("unchecked")
        List<ConfirmedBookingPaymentDto> dtoList = (List<ConfirmedBookingPaymentDto>) model.getAttribute("confirmedBookings");
        assertNotNull(dtoList);
        // Only confirmed booking must be present; pending booking must be strictly excluded
        assertEquals(1, dtoList.size());
        assertEquals(100L, dtoList.get(0).getBookingId());
        assertEquals("Toyota Prius 2024", dtoList.get(0).getVehicleModel());
        assertEquals("CONFIRMED", dtoList.get(0).getBookingStatus());
    }

    @Test
    @DisplayName("processPayment succeeds with valid 16-digit card, 3-digit CVV, and MM/YY expiry")
    void testProcessPayment_Success() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(100L)).thenReturn(confirmedBooking);
        when(paymentService.getInvoiceById(50L)).thenReturn(invoice);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                100L,
                50L,
                "Commercial Bank",
                "4111 2222 3333 4444", // 16 digits with spaces
                "12/28",
                "123",
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments", view);
        assertNotNull(redirectAttributes.getFlashAttributes().get("successMessage"));
        assertEquals("PAID", invoice.getStatus());
        verify(paymentService, times(1)).processPayment(any(CreditCardPay.class));
        verify(paymentService, times(1)).updateInvoice(eq(50L), eq(invoice));
    }

    @Test
    @DisplayName("processPayment rejects card numbers that do not have 16 digits")
    void testProcessPayment_InvalidCardLength() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(100L)).thenReturn(confirmedBooking);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                100L,
                50L,
                "Commercial Bank",
                "4111 2222 3333", // only 12 digits
                "12/28",
                "123",
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments?selectedBookingId=100", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("16-digit"));
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("processPayment rejects invalid CVV that is not 3 digits")
    void testProcessPayment_InvalidCvv() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(100L)).thenReturn(confirmedBooking);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                100L,
                50L,
                "Commercial Bank",
                "4111222233334444",
                "12/28",
                "12", // only 2 digits
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments?selectedBookingId=100", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("CVV"));
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("processPayment rejects unapproved pending booking")
    void testProcessPayment_UnapprovedBookingRejected() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(101L)).thenReturn(pendingBooking);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                101L,
                null,
                "Commercial Bank",
                "4111222233334444",
                "12/28",
                "123",
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("must be approved by staff"));
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("processPayment rejects invalid expiry month greater than 12")
    void testProcessPayment_InvalidExpiryMonth() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(100L)).thenReturn(confirmedBooking);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                100L,
                50L,
                "Commercial Bank",
                "4111222233334444",
                "13/25", // Invalid month 13 > 12
                "123",
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments?selectedBookingId=100", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("Month must be between 01 and 12"));
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    @DisplayName("processPayment succeeds with valid PayPal email via Strategy Pattern")
    void testProcessPayment_PayPalSuccess() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(100L)).thenReturn(confirmedBooking);
        when(paymentService.getInvoiceById(50L)).thenReturn(invoice);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                100L,
                50L,
                "PAYPAL",
                null,
                null,
                null,
                null,
                "customer.driveflow@paypal.com",
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments", view);
        assertNotNull(redirectAttributes.getFlashAttributes().get("successMessage"));
        assertTrue(redirectAttributes.getFlashAttributes().get("successMessage").toString().contains("PayPal"));
        assertEquals("PAID", invoice.getStatus());
        verify(paymentService, times(1)).processCustomerPayment(eq("PAYPAL"), anyDouble(), eq("100"));
        verify(paymentService, times(1)).processPayment(any(Payment.class));
        verify(paymentService, times(1)).updateInvoice(eq(50L), eq(invoice));
    }

    @Test
    @DisplayName("processPayment rejects invalid PayPal email")
    void testProcessPayment_PayPalInvalidEmail() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@driveflow.com");
        when(staffRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.empty());
        when(userService.findCustomerByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getBookingById(100L)).thenReturn(confirmedBooking);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.processPayment(
                100L,
                50L,
                "PAYPAL",
                null,
                null,
                null,
                null,
                "not-an-email",
                authentication,
                redirectAttributes
        );

        assertEquals("redirect:/invoices-payments?selectedBookingId=100", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("PayPal"));
        verify(paymentService, never()).processPayment(any());
    }
}
