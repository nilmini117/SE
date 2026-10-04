package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Controller for the dedicated "Invoices & Payments" customer portal tab.
 * 
 * Rules:
 * 1. Renders the migrated "My Rental Bookings" table component strictly for
 * bookings
 * that have been approved/confirmed by staff (Status: CONFIRMED or APPROVED).
 * 2. Integrates the payment gateway form (requiring a 16-digit credit card,
 * 3-digit CVV, and MM/YY expiry)
 * directly into this view for confirmed bookings.
 */
@Controller
public class CustomerInvoicePaymentController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private com.driveflow.demo_driveflow.users.StaffRepository staffRepository;

    /**
     * Dedicated "Invoices & Payments" top-level portal tab.
     */
    @GetMapping({ "/invoices-payments", "/customer/invoices-payments", "/customer/invoices", "/profile/invoices" })
    public String viewInvoicesAndPayments(
            @RequestParam(value = "selectedBookingId", required = false) Long selectedBookingId,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (staffRepository.findByEmail(email).isPresent()) {
            return "redirect:/payments"; // Redirect staff to corporate payments & sales view
        }

        Customer customer = userService.findCustomerByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer account not found for: " + email));

        // Fetch customer bookings and filter strictly for approved/confirmed bookings
        List<Booking> allBookings = bookingService.getBookingsByCustomer(customer);
        List<ConfirmedBookingPaymentDto> dtoList = buildConfirmedBookingDtos(allBookings);

        // Pre-select requested booking or the first unpaid booking
        ConfirmedBookingPaymentDto selectedItem = null;
        if (selectedBookingId != null) {
            selectedItem = dtoList.stream()
                    .filter(d -> d.getBookingId().equals(selectedBookingId))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedItem == null) {
            selectedItem = dtoList.stream()
                    .filter(d -> !d.isPaid())
                    .findFirst()
                    .orElse(null);
        }
        if (selectedItem == null && !dtoList.isEmpty()) {
            selectedItem = dtoList.get(0);
        }

        List<Payment> customerPayments = paymentService.getPaymentsByCustomer(customer);

        long unpaidCount = dtoList.stream().filter(d -> !d.isPaid()).count();
        long paidCount = dtoList.stream().filter(ConfirmedBookingPaymentDto::isPaid).count();

        model.addAttribute("activeTab", "invoices-payments");
        model.addAttribute("customer", customer);
        model.addAttribute("confirmedBookings", dtoList);
        model.addAttribute("selectedItem", selectedItem);
        model.addAttribute("payments", customerPayments);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("paidCount", paidCount);

        return "customer/invoices-payments";
    }

    /**
     * Form submission for the integrated Payment Gateway in the Invoices & Payments
     * tab.
     */
    @PostMapping("/invoices-payments/pay")
    public String processPayment(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam(value = "invoiceId", required = false) Long invoiceId,
            @RequestParam("bankName") String bankName,
            @RequestParam("cardNo") String cardNo,
            @RequestParam(value = "expiry", required = false) String expiry,
            @RequestParam(value = "cvv", required = false) String cvv,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (staffRepository.findByEmail(email).isPresent()) {
            return "redirect:/payments";
        }

        Customer customer = userService.findCustomerByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || booking.getCustomer() == null
                || !booking.getCustomer().getSystemId().equals(customer.getSystemId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unauthorized: Booking does not belong to your account.");
            return "redirect:/invoices-payments";
        }

        // Rule: Customers can only pay for bookings approved/confirmed by staff,
        // active, or returned
        String status = booking.getStatus();
        if (status == null || (!status.equalsIgnoreCase("CONFIRMED") && !status.equalsIgnoreCase("APPROVED") &&
                !status.equalsIgnoreCase("ACTIVE") && !status.equalsIgnoreCase("RETURNED"))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Payment Rejected: Reservation #BK-" + bookingId + " is currently " + status
                            + " and must be approved by staff before payment can be accepted.");
            return "redirect:/invoices-payments";
        }

        // 16-Digit Credit Card Validation
        String cleanCardNo = (cardNo != null) ? cardNo.replaceAll("[\\s-]", "") : "";
        if (cleanCardNo.length() != 16 || !cleanCardNo.matches("^\\d{16}$")) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid Card Number: Must enter a valid 16-digit credit or debit card number.");
            redirectAttributes.addFlashAttribute("errorBookingId", bookingId);
            return "redirect:/invoices-payments?selectedBookingId=" + bookingId;
        }

        // 3-Digit CVV Validation
        String cleanCvv = (cvv != null) ? cvv.trim() : "";
        if (!cleanCvv.matches("^\\d{3}$") && !cleanCvv.matches("^\\d{4}$")) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid CVV: Security code must be 3 digits.");
            redirectAttributes.addFlashAttribute("errorBookingId", bookingId);
            return "redirect:/invoices-payments?selectedBookingId=" + bookingId;
        }

        // Expiry Date Validation (MM/YY)
        String cleanExpiry = (expiry != null) ? expiry.trim() : "";
        if (!cleanExpiry.matches("^(0[1-9]|1[0-2])\\/\\d{2}$")) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid Expiration Date: Please provide a valid MM/YY format (e.g. 12/28).");
            redirectAttributes.addFlashAttribute("errorBookingId", bookingId);
            return "redirect:/invoices-payments?selectedBookingId=" + bookingId;
        }

        // Fetch or create invoice
        Invoice invoice = null;
        if (invoiceId != null) {
            invoice = paymentService.getInvoiceById(invoiceId);
        }
        if (invoice == null) {
            Optional<Invoice> invOpt = invoiceRepository.findByBooking(booking);
            invoice = invOpt.orElseGet(() -> {
                Invoice newInv = new Invoice();
                newInv.setBooking(booking);
                newInv.setRentalAmt(booking.getChargedRate() != null ? booking.getChargedRate() : BigDecimal.ZERO);
                newInv.setLateFee(BigDecimal.ZERO);
                newInv.setTotalAmt(booking.getChargedRate() != null ? booking.getChargedRate() : BigDecimal.ZERO);
                newInv.setInvoiceDate(LocalDate.now());
                newInv.setStatus("UNPAID");
                return paymentService.generateInvoice(newInv);
            });
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            redirectAttributes.addFlashAttribute("infoMessage",
                    "Invoice #INV-" + invoice.getInvoiceId() + " for Booking #BK-" + bookingId
                            + " has already been settled.");
            return "redirect:/invoices-payments";
        }

        // Execute payment transaction
        CreditCardPay cc = new CreditCardPay();
        cc.setBankName(bankName != null && !bankName.isBlank() ? bankName.trim() : "Direct Card Payment");
        cc.setCardNo(cleanCardNo);
        cc.setAmountPaid(invoice.getTotalAmt());
        cc.setPaymentDate(LocalDate.now());
        cc.setStatus("COMPLETED");
        cc.setRefNo("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cc.setInvoice(invoice);

        paymentService.processPayment(cc);

        invoice.setStatus("PAID");
        paymentService.updateInvoice(invoice.getInvoiceId(), invoice);

        redirectAttributes.addFlashAttribute("successMessage",
                "Payment of $" + String.format("%.2f", invoice.getTotalAmt()) + " processed successfully! " +
                        "Booking #BK-" + bookingId + " (Invoice #INV-" + invoice.getInvoiceId()
                        + ") is now fully PAID. Transaction Ref: " + cc.getRefNo());

        return "redirect:/invoices-payments";
    }

    /**
     * REST endpoint returning confirmed bookings and payment states for React
     * dashboard.
     */
    @GetMapping("/api/customer/confirmed-bookings")
    @ResponseBody
    public ResponseEntity<List<ConfirmedBookingPaymentDto>> getConfirmedBookingsApi(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        Optional<Customer> customerOpt = userService.findCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        List<Booking> allBookings = bookingService.getBookingsByCustomer(customerOpt.get());
        List<ConfirmedBookingPaymentDto> dtoList = buildConfirmedBookingDtos(allBookings);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * REST endpoint for processing customer card payment via React frontend.
     */
    @PostMapping("/api/customer/pay")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPaymentApi(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            response.put("success", false);
            response.put("message", "Authentication required");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Long bookingId = Long.parseLong(payload.get("bookingId"));
            String bankName = payload.getOrDefault("bankName", "Commercial Bank");
            String cardNo = payload.getOrDefault("cardNo", "");
            String expiry = payload.getOrDefault("expiry", "");
            String cvv = payload.getOrDefault("cvv", "");

            String cleanCardNo = cardNo.replaceAll("[\\s-]", "");
            if (cleanCardNo.length() != 16 || !cleanCardNo.matches("^\\d{16}$")) {
                response.put("success", false);
                response.put("message", "Invalid card: must be a 16-digit credit card number");
                return ResponseEntity.badRequest().body(response);
            }

            if (!cvv.trim().matches("^\\d{3,4}$")) {
                response.put("success", false);
                response.put("message", "Invalid CVV: must be a 3-digit security code");
                return ResponseEntity.badRequest().body(response);
            }
            // Validates MM/YY or MM/YYYY format
            if (!expiry.trim().matches("^(0[1-9]|1[0-2])/?([0-9]{2}|[0-9]{4})$")) {
                response.put("success", false);
                response.put("message", "Invalid expiry date: must be in MM/YY or MM/YYYY format");
                return ResponseEntity.badRequest().body(response);
            }

            Booking booking = bookingService.getBookingById(bookingId);
            String bStatus = booking != null ? booking.getStatus() : null;
            if (bStatus == null || (!"CONFIRMED".equalsIgnoreCase(bStatus) && !"APPROVED".equalsIgnoreCase(bStatus) &&
                    !"ACTIVE".equalsIgnoreCase(bStatus) && !"RETURNED".equalsIgnoreCase(bStatus))) {
                response.put("success", false);
                response.put("message", "Booking is not in payable status (must be approved, active, or returned).");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Invoice> invOpt = invoiceRepository.findByBooking(booking);
            Invoice invoice = invOpt.orElseGet(() -> {
                Invoice newInv = new Invoice();
                newInv.setBooking(booking);
                newInv.setRentalAmt(booking.getChargedRate());
                newInv.setTotalAmt(booking.getChargedRate());
                newInv.setInvoiceDate(LocalDate.now());
                newInv.setStatus("UNPAID");
                return paymentService.generateInvoice(newInv);
            });

            CreditCardPay cc = new CreditCardPay();
            cc.setBankName(bankName);
            cc.setCardNo(cleanCardNo);
            cc.setAmountPaid(invoice.getTotalAmt());
            cc.setPaymentDate(LocalDate.now());
            cc.setStatus("COMPLETED");
            cc.setRefNo("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            cc.setInvoice(invoice);

            paymentService.processPayment(cc);
            invoice.setStatus("PAID");
            paymentService.updateInvoice(invoice.getInvoiceId(), invoice);

            response.put("success", true);
            response.put("refNo", cc.getRefNo());
            response.put("amountPaid", invoice.getTotalAmt());
            response.put("message", "Payment processed successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Payment failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private List<ConfirmedBookingPaymentDto> buildConfirmedBookingDtos(List<Booking> bookings) {
        List<ConfirmedBookingPaymentDto> dtoList = new ArrayList<>();
        if (bookings == null)
            return dtoList;

        for (Booking b : bookings) {
            String status = b.getStatus();
            // Render bookings that are CONFIRMED, APPROVED, ACTIVE, or RETURNED
            if (status != null && ("CONFIRMED".equalsIgnoreCase(status.trim()) ||
                    "APPROVED".equalsIgnoreCase(status.trim()) ||
                    "ACTIVE".equalsIgnoreCase(status.trim()) ||
                    "RETURNED".equalsIgnoreCase(status.trim()))) {
                Optional<Invoice> invOpt = invoiceRepository.findByBooking(b);
                Invoice inv = invOpt.orElse(null);
                if (inv == null) {
                    try {
                        Invoice newInv = new Invoice();
                        newInv.setBooking(b);
                        newInv.setRentalAmt(b.getChargedRate() != null ? b.getChargedRate() : BigDecimal.ZERO);
                        newInv.setLateFee(BigDecimal.ZERO);
                        newInv.setTotalAmt(b.getChargedRate() != null ? b.getChargedRate() : BigDecimal.ZERO);
                        newInv.setInvoiceDate(LocalDate.now());
                        newInv.setStatus("UNPAID");
                        inv = paymentService.generateInvoice(newInv);
                    } catch (Exception ignored) {
                    }
                }

                boolean isPaid = inv != null && "PAID".equalsIgnoreCase(inv.getStatus());
                BigDecimal amountDue = (inv != null && inv.getTotalAmt() != null)
                        ? inv.getTotalAmt()
                        : (b.getChargedRate() != null ? b.getChargedRate() : BigDecimal.ZERO);

                String cleanStatus = status.trim().toUpperCase();
                dtoList.add(ConfirmedBookingPaymentDto.builder()
                        .bookingId(b.getBookingId())
                        .vehicleModel(b.getVehicle() != null ? b.getVehicle().getModel() : "Vehicle")
                        .vehicleRegNo(b.getVehicle() != null ? b.getVehicle().getRegNo() : "")
                        .pickupDate(b.getBookingDate())
                        .returnDate(b.getEndDate())
                        .duration(b.getDuration())
                        .chargedRate(b.getChargedRate())
                        .bookingStatus(cleanStatus)
                        .staffMessage(b.getStaffMessage() != null ? b.getStaffMessage()
                                : ("RETURNED".equals(cleanStatus) ? "Vehicle returned" : "Staff Confirmed"))
                        .invoiceId(inv != null ? inv.getInvoiceId() : null)
                        .invoiceStatus(inv != null ? inv.getStatus() : "UNPAID")
                        .rentalAmount(inv != null ? inv.getRentalAmt() : b.getChargedRate())
                        .lateFee(inv != null ? inv.getLateFee() : BigDecimal.ZERO)
                        .totalAmountDue(amountDue)
                        .isPaid(isPaid)
                        .build());
            }
        }
        return dtoList;
    }
}
