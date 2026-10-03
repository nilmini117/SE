package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.booking.exception.ActiveBookingLimitExceededException;
import com.driveflow.demo_driveflow.booking.exception.BookingCancellationNotAllowedException;
import com.driveflow.demo_driveflow.booking.exception.BookingImmutabilityException;
import com.driveflow.demo_driveflow.booking.exception.BranchSelectionRequiredException;
import com.driveflow.demo_driveflow.booking.pricing.PricingBreakdown;
import com.driveflow.demo_driveflow.booking.pricing.PricingEngineService;
import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.promotion.Promotion;
import com.driveflow.demo_driveflow.promotion.PromotionRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import com.driveflow.demo_driveflow.vehicle.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    public static final String MODULE_TITLE = "Pick your choice in our park";

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PricingEngineService pricingEngineService;

    @Autowired
    private PromotionRepository promotionRepository;

    private boolean isStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        boolean hasStaffRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"));
        if (hasStaffRole) {
            return true;
        }
        return staffRepository.findByEmail(authentication.getName()).isPresent();
    }

    @GetMapping
    public String listBookings(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "success", required = false) String success,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        boolean staff = isStaff(authentication);
        String email = authentication.getName();
        model.addAttribute("isStaff", staff);
        model.addAttribute("moduleTitle", MODULE_TITLE);

        if (error != null && !model.containsAttribute("errorMessage")) {
            if ("unauthorized".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "You are not authorized to perform this booking action.");
            } else if ("cannot-cancel".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Cancellation action is disabled for approved reservations. Only pending bookings can be cancelled.");
            } else if ("conflict".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Conflict Error: A customer can only hold exactly 1 active booking at a time.");
            } else {
                model.addAttribute("errorMessage", "Booking operation error: " + error);
            }
        }
        if (success != null && !model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", success);
        }

        List<Booking> bookingsList;
        if (staff) {
            String activeStatus = (status == null || status.isBlank()) ? "PENDING" : status.trim().toUpperCase();
            model.addAttribute("selectedStatus", activeStatus);
            model.addAttribute("statusCounts", bookingService.getBookingStatusCounts());
            bookingsList = bookingService.getBookingsByStatus(activeStatus);
        } else {
            Optional<Customer> customerOpt = customerRepository.findByEmail(email);
            if (customerOpt.isPresent()) {
                Customer cust = customerOpt.get();
                bookingsList = bookingService.getBookingsByCustomer(cust);
                model.addAttribute("currentCustomer", cust);
                model.addAttribute("activeBookingCount", bookingService.getActiveBookingCount(cust.getSystemId()));
                model.addAttribute("hasActiveBooking", bookingService.hasActiveBooking(cust.getSystemId()));
            } else {
                bookingsList = List.of();
            }
        }
        model.addAttribute("bookings", bookingsList);

        Map<Long, Invoice> bookingInvoices = new HashMap<>();
        for (Booking b : bookingsList) {
            invoiceRepository.findByBooking(b).ifPresent(inv -> bookingInvoices.put(b.getBookingId(), inv));
        }
        model.addAttribute("bookingInvoices", bookingInvoices);

        return "booking/booking-list";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "branchId", required = false) Long branchId,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff accounts cannot reserve vehicles. Customer accounts only.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        if (customerOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A verified customer profile is required to book a vehicle.");
            return "redirect:/bookings";
        }

        Customer currentCustomer = customerOpt.get();

        // Concurrency Limit Check before rendering form:
        long activeCount = bookingService.getActiveBookingCount(currentCustomer.getSystemId());
        if (activeCount > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Active Booking Limit Exceeded: You currently have " + activeCount +
                    " active booking in progress. Under our park policy, each customer can only book exactly one vehicle at a time.");
            return "redirect:/bookings";
        }

        Booking booking = new Booking();
        LocalDate today = LocalDate.now();
        LocalDate defaultEnd = today.plusDays(3);
        booking.setBookingDate(today);
        booking.setEndDate(defaultEnd);
        booking.setDuration(3);
        booking.setQuantity(1);
        booking.setStatus("PENDING");
        booking.setCustomer(currentCustomer);

        List<Branch> branches = branchRepository.findAll();
        model.addAttribute("branches", branches);

        Branch selectedBranch = null;
        Vehicle selectedVehicle = null;

        if (branchId != null) {
            selectedBranch = branchRepository.findById(branchId).orElse(null);
        }

        if (vehicleId != null) {
            try {
                Vehicle v = vehicleService.getVehicleById(vehicleId);
                selectedVehicle = v;
                booking.setVehicle(v);
                if (selectedBranch == null && v.getBranch() != null) {
                    selectedBranch = v.getBranch();
                }
            } catch (Exception ignored) {}
        }

        if (selectedBranch != null) {
            booking.setPickupBranch(selectedBranch);
            booking.setReturnBranch(selectedBranch);
        }

        // Fetch active seasonal promotions
        List<Promotion> activePromotions = promotionRepository.findActivePromotions(today);
        model.addAttribute("activePromotions", activePromotions);

        // Calculate initial pricing breakdown
        Long selBranchId = selectedBranch != null ? selectedBranch.getBranchId() : (branches.isEmpty() ? null : branches.get(0).getBranchId());
        Long selVehId = selectedVehicle != null ? selectedVehicle.getVehicleId() : null;
        PricingBreakdown initialBreakdown = pricingEngineService.calculatePricing(selVehId, selBranchId, today, defaultEnd, null);
        booking.setChargedRate(initialBreakdown.getFinalTotalCost());

        model.addAttribute("booking", booking);
        model.addAttribute("selectedBranch", selectedBranch);
        model.addAttribute("selectedVehicle", selectedVehicle);
        model.addAttribute("currentCustomer", currentCustomer);
        model.addAttribute("pricingBreakdown", initialBreakdown);
        model.addAttribute("moduleTitle", MODULE_TITLE);
        model.addAttribute("isStaff", false);

        // Vehicles stationed at selected pickup branch (if branch selected)
        if (selectedBranch != null) {
            model.addAttribute("vehicles", vehicleRepository.findAvailableByBranchId(selectedBranch.getBranchId()));
        } else {
            model.addAttribute("vehicles", List.of());
        }
        model.addAttribute("allAvailableVehicles", vehicleRepository.findByStatus("AVAILABLE"));

        return "booking/booking-form";
    }

    @PostMapping
    public String createBooking(
            @ModelAttribute Booking booking,
            @RequestParam(value = "pickupBranchId", required = false) Long pickupBranchId,
            @RequestParam(value = "returnBranchId", required = false) Long returnBranchId,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "couponCode", required = false) String couponCode,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff members cannot place vehicle reservations.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Optional<Customer> currentCustomerOpt = customerRepository.findByEmail(email);
        if (currentCustomerOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A registered customer account is required to place a reservation.");
            return "redirect:/bookings";
        }
        Customer customer = currentCustomerOpt.get();
        booking.setCustomer(customer);

        // 1. Mandatory Branch Selection Check:
        if (pickupBranchId == null || pickupBranchId <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Mandatory Branch Selection: You must select a specific pickup branch before validating vehicle availability.");
            return "redirect:/bookings/new" + (vehicleId != null ? "?vehicleId=" + vehicleId : "");
        }

        Branch pickupBranch = branchRepository.findById(pickupBranchId).orElse(null);
        if (pickupBranch == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "The selected pickup branch does not exist.");
            return "redirect:/bookings/new";
        }
        booking.setPickupBranch(pickupBranch);

        if (returnBranchId != null && returnBranchId > 0) {
            Branch returnBranch = branchRepository.findById(returnBranchId).orElse(pickupBranch);
            booking.setReturnBranch(returnBranch);
        } else {
            booking.setReturnBranch(pickupBranch);
        }

        // 2. Strict Concurrency Limit Check:
        long activeBookings = bookingService.getActiveBookingCount(customer.getSystemId());
        if (activeBookings > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Conflict Error: Customer already has " + activeBookings +
                    " active booking(s). A single customer can only book exactly one vehicle at a time.");
            return "redirect:/bookings";
        }

        // 3. Vehicle Selection Check:
        if (vehicleId == null || vehicleId <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an available vehicle stationed at the chosen branch.");
            return "redirect:/bookings/new?branchId=" + pickupBranchId;
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected vehicle not found.");
            return "redirect:/bookings/new?branchId=" + pickupBranchId;
        }
        booking.setVehicle(vehicle);

        // Validate vehicle is at selected pickup branch
        if (vehicle.getBranch() != null && !vehicle.getBranch().getBranchId().equals(pickupBranch.getBranchId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Selected vehicle '" + vehicle.getModel() + "' is not stationed at '" + pickupBranch.getBranchName() + "'.");
            return "redirect:/bookings/new?branchId=" + pickupBranchId;
        }

        if (vehicle.getStatus() != null && !"AVAILABLE".equalsIgnoreCase(vehicle.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Selected vehicle '" + vehicle.getModel() + "' is not available (Status: " + vehicle.getStatus() + ").");
            return "redirect:/bookings/new?branchId=" + pickupBranchId;
        }

        // Dates & Duration
        LocalDate start = booking.getBookingDate() != null ? booking.getBookingDate() : LocalDate.now();
        LocalDate end = booking.getEndDate() != null ? booking.getEndDate() : start.plusDays(1);
        if (end.isBefore(start)) {
            end = start;
        }
        booking.setBookingDate(start);
        booking.setEndDate(end);

        // 4. Pricing Engine calculation: apply discount to base rate & expose final calculated payment
        PricingBreakdown pricing = pricingEngineService.calculatePricing(
                vehicle.getVehicleId(),
                pickupBranch.getBranchId(),
                start,
                end,
                couponCode
        );
        booking.setChargedRate(pricing.getFinalTotalCost());
        booking.setDuration(pricing.getDurationDays());
        booking.setQuantity(1);

        // 5. Payment Gate:
        // Do not trigger the payment gateway during the initial booking submission.
        // Status remains PENDING until approved by staff.
        booking.setStatus("PENDING");

        try {
            bookingService.createBooking(booking);
            String successMsg = "Vehicle reservation submitted successfully under '" + MODULE_TITLE + "'! " +
                    "Status: PENDING staff approval. 🔒 Payment gate is locked until approval.";
            if (pricing.isPromotionApplied()) {
                successMsg += " Applied seasonal promotion: " + pricing.getPromotionTitle() +
                        " (-" + pricing.getDiscountRate() + "% off).";
            }
            redirectAttributes.addFlashAttribute("successMessage", successMsg);
            return "redirect:/bookings";
        } catch (ActiveBookingLimitExceededException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Conflict Error: " + e.getMessage());
            return "redirect:/bookings";
        } catch (BranchSelectionRequiredException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reservation failed: " + e.getMessage());
            return "redirect:/bookings/new?branchId=" + pickupBranchId;
        }
    }

    // --- Strict Immutability Guard ---
    // Once the booking payload is submitted and the record is created,
    // lock the record from any customer-initiated edits (updates to dates, vehicles, or branches are strictly prohibited).

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Immutability Locked: Booking records cannot be edited once created. Customer updates to dates, vehicles, or branches are strictly prohibited.");
        return "redirect:/bookings";
    }

    @PostMapping("/{id}")
    public String updateBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Immutability Locked: Booking records cannot be modified once submitted. Updates to dates, vehicles, or branches are strictly prohibited.");
        return "redirect:/bookings";
    }

    // --- Cancellation Flow with State Check ---
    // Customers can only trigger a cancellation if the booking status is PENDING (before staff approval).
    // If APPROVED (or CONFIRMED), disable the cancel action.

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public String cancelBooking(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff should use the Decline action to decline reservations.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Booking existing;
        try {
            existing = bookingService.getBookingById(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to cancel this booking.");
            return "redirect:/bookings";
        }

        // State Check: Only PENDING bookings can be cancelled by customer
        if (!"PENDING".equalsIgnoreCase(existing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cancellation Action Disabled: Booking #BK-" + id + " has status '" + existing.getStatus() +
                    "'. Customers can only cancel reservations in PENDING status prior to staff approval.");
            return "redirect:/bookings";
        }

        try {
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking #BK-" + id + " has been successfully cancelled prior to staff approval.");
        } catch (BookingCancellationNotAllowedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/bookings";
    }

    // --- Staff Actions: Approve and Decline ---

    @GetMapping("/{id}/approve")
    public String approveBooking(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only staff members can approve vehicle reservations.");
            return "redirect:/bookings";
        }

        bookingService.approveBooking(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Booking #BK-" + id + " has been APPROVED! Staff confirmation sent and payment gate is now unlocked for customer.");
        return "redirect:/bookings?status=PENDING";
    }

    @PostMapping("/{id}/decline")
    public String declineBooking(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only staff can decline reservations.");
            return "redirect:/bookings";
        }

        if (reason == null || reason.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A decline reason is mandatory so the customer is notified.");
            return "redirect:/bookings?status=PENDING";
        }

        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        // Rule: Staff cannot decline an approved booking
        if ("CONFIRMED".equalsIgnoreCase(existing.getStatus()) || "APPROVED".equalsIgnoreCase(existing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Action Prohibited: Staff cannot decline an approved booking (#BK-" + id + ").");
            return "redirect:/bookings?status=" + existing.getStatus();
        }

        // Rule: Staff cannot decline a paid booking
        Optional<Invoice> invOpt = invoiceRepository.findByBooking(existing);
        if (invOpt.isPresent() && "PAID".equalsIgnoreCase(invOpt.get().getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Action Prohibited: Staff cannot decline a paid booking (#BK-" + id + ").");
            return "redirect:/bookings?status=" + existing.getStatus();
        }

        bookingService.declineBooking(id, reason.trim());
        redirectAttributes.addFlashAttribute("successMessage",
                "Booking #BK-" + id + " has been declined and customer notified: \"" + reason.trim() + "\"");
        return "redirect:/bookings?status=PENDING";
    }

    @PostMapping("/{id}/edit-status")
    public String editBookingStatus(
            @PathVariable Long id,
            @RequestParam("status") String newStatus,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only staff members can update booking status.");
            return "redirect:/bookings";
        }

        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        // Rule: Staff can edit status ONLY if NOT paid by customer
        Optional<Invoice> invOpt = invoiceRepository.findByBooking(existing);
        if (invOpt.isPresent() && "PAID".equalsIgnoreCase(invOpt.get().getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Status Modification Blocked: Booking #BK-" + id + " has already been paid by the customer. Status changes are prohibited once paid.");
            return "redirect:/bookings";
        }

        try {
            bookingService.updateBookingStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking #BK-" + id + " status successfully updated to " + newStatus.toUpperCase() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + e.getMessage());
        }

        return "redirect:/bookings";
    }

    /**
     * Customer Vehicle Return Action (MVC fallback):
     * Updates booking to RETURNED, releases vehicle to AVAILABLE, and redirects to unlocked feedback form.
     */
    @PostMapping("/{id}/return")
    public String returnVehicle(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/invoices-payments";
        }

        if (!isStaff(authentication)) {
            String email = authentication.getName();
            Optional<Customer> customerOpt = customerRepository.findByEmail(email);
            if (customerOpt.isEmpty() || existing.getCustomer() == null ||
                    !existing.getCustomer().getSystemId().equals(customerOpt.get().getSystemId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to return this vehicle.");
                return "redirect:/invoices-payments";
            }
        }

        try {
            bookingService.returnVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Vehicle successfully returned! Inventory is now released to AVAILABLE, and feedback has been unlocked for your trip.");
            return "redirect:/feedback/new?bookingId=" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Return Vehicle failed: " + e.getMessage());
            return "redirect:/invoices-payments";
        }
    }

    // --- Dynamic REST API Endpoints for Frontend Engine ---

    @GetMapping("/calculate-pricing")
    @ResponseBody
    public ResponseEntity<PricingBreakdown> calculatePricing(
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "pickupBranchId", required = false) Long pickupBranchId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "couponCode", required = false) String couponCode) {

        PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                vehicleId, pickupBranchId, startDate, endDate, couponCode);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/vehicles-by-branch")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getVehiclesByBranch(@RequestParam("branchId") Long branchId) {
        List<Vehicle> list = vehicleRepository.findAvailableByBranchId(branchId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Vehicle v : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("vehicleId", v.getVehicleId());
            map.put("model", v.getModel());
            map.put("regNo", v.getRegNo());
            map.put("color", v.getColor());
            map.put("mileage", v.getMileage());
            map.put("status", v.getStatus());
            map.put("displayName", v.getModel() + " [" + v.getRegNo() + "] - " + v.getColor());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
