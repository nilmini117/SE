package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

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
    public String listFeedback(Model model, Authentication authentication) {
        boolean staff = isStaff(authentication);
        model.addAttribute("isStaff", staff);

        if (staff) {
            model.addAttribute("feedbackList", feedbackService.getAllFeedback());
            model.addAttribute("hasCompletedBooking", false);
        } else if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            Optional<Customer> customerOpt = customerRepository.findByEmail(email);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                model.addAttribute("feedbackList", feedbackService.getFeedbackByCustomer(customer));
                List<Booking> completedBookings = bookingService.getCompletedBookingsForCustomer(customer.getSystemId());
                boolean hasCompleted = !completedBookings.isEmpty();
                model.addAttribute("hasCompletedBooking", hasCompleted);
                if (hasCompleted) {
                    model.addAttribute("recentBooking", completedBookings.get(0));
                }
            } else {
                model.addAttribute("feedbackList", List.of());
                model.addAttribute("hasCompletedBooking", false);
            }
        } else {
            model.addAttribute("feedbackList", List.of());
            model.addAttribute("hasCompletedBooking", false);
        }
        return "feedback/feedback-list";
    }

    @GetMapping("/new")
    public String showSubmitForm(
            @RequestParam(value = "bookingId", required = false) Long bookingId,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff members do not submit feedback.");
            return "redirect:/feedback";
        }

        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer profile not found.");
            return "redirect:/feedback";
        }

        // Submission Lock: Customers can only submit feedback after their vehicle has been returned (Booking status = COMPLETED or RETURNED)
        Booking targetBooking = null;
        if (bookingId != null) {
            Booking requested = bookingService.getBookingById(bookingId);
            if (requested != null && requested.getCustomer() != null && requested.getCustomer().getSystemId().equals(customer.getSystemId())) {
                String s = requested.getStatus();
                if (s != null && ("RETURNED".equalsIgnoreCase(s.trim()) || "COMPLETED".equalsIgnoreCase(s.trim()))) {
                    targetBooking = requested;
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Feedback submission is locked for booking #BK-" + bookingId + ". You can only submit feedback after your vehicle has been returned (Booking status: RETURNED or COMPLETED).");
                    return "redirect:/invoices-payments";
                }
            }
        }

        if (targetBooking == null) {
            targetBooking = bookingService.getLastCompletedBookingForCustomer(customer.getSystemId());
        }

        if (targetBooking == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Feedback submission is locked. You can only submit feedback after your vehicle has been returned (Booking status: COMPLETED or RETURNED).");
            return "redirect:/feedback";
        }

        Feedback feedback = new Feedback();
        feedback.setDate(LocalDate.now());
        feedback.setStatus("OPEN");
        feedback.setApprovalStatus("PENDING");
        feedback.setPublicVisibility(false);
        feedback.setBooking(targetBooking);

        model.addAttribute("feedback", feedback);
        model.addAttribute("booking", targetBooking);
        model.addAttribute("recentBooking", targetBooking);
        return "feedback/feedback-form";
    }

    @PostMapping
    public String submitFeedback(
            @ModelAttribute Feedback feedback,
            @RequestParam(value = "bookingId", required = false) Long bookingId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        if (isStaff(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff members cannot submit customer feedback.");
        }

        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer record not found."));

        try {
            feedbackService.submitCustomerFeedback(feedback, customer, bookingId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your feedback has been submitted successfully and is pending review.");
            return "redirect:/feedback";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/feedback";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        // Staff Permissions (Immutability): Staff must have strictly read-only access to feedback text.
        if (isStaff(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff members have strictly read-only access to feedback text.");
        }

        Feedback feedback = feedbackService.getFeedbackById(id);
        String email = authentication.getName();
        if (feedback.getCustomer() == null || !feedback.getCustomer().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to edit this feedback.");
        }

        // Customer Permissions: Customers can edit their feedback only if it has not yet been approved by staff.
        if (feedback.isApproved()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This feedback has already been approved by staff and cannot be modified.");
            return "redirect:/feedback";
        }

        model.addAttribute("feedback", feedback);
        model.addAttribute("recentBooking", feedback.getBooking());
        return "feedback/feedback-form";
    }

    @PostMapping("/{id}")
    public String updateFeedback(
            @PathVariable Long id,
            @ModelAttribute Feedback feedback,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication != null && isStaff(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff members cannot alter feedback text.");
        }

        Feedback existing = feedbackService.getFeedbackById(id);
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            if (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this feedback.");
            }
        }

        if (existing.isApproved()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback has already been approved by staff and cannot be modified.");
        }

        try {
            feedbackService.updateFeedback(id, feedback);
            redirectAttributes.addFlashAttribute("successMessage", "Your feedback has been updated successfully.");
            return "redirect:/feedback";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/feedback";
        }
    }

    /**
     * PUT Endpoint for updating feedback text.
     * Enforces that Staff-role callers are rejected with 403 Forbidden.
     */
    @PutMapping(value = "/{id}")
    @ResponseBody
    public ResponseEntity<?> updateFeedbackPut(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload,
            Authentication authentication) {
        if (authentication != null && isStaff(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "error", "Forbidden", "message", "Staff members have strictly read-only access to feedback text."));
        }

        Feedback existing = feedbackService.getFeedbackById(id);
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            if (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", 403, "error", "Forbidden", "message", "You are not authorized to edit this feedback."));
            }
        }

        if (existing.isApproved()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "error", "Bad Request", "message", "Approved feedback is locked and cannot be edited."));
        }

        if (payload != null) {
            if (payload.containsKey("category")) {
                existing.setCategory((String) payload.get("category"));
            }
            if (payload.containsKey("message")) {
                existing.setMessage((String) payload.get("message"));
            }
        }
        Feedback updated = feedbackService.save(existing);
        return ResponseEntity.ok(updated);
    }

    /**
     * Staff Visibility Action: The only action staff can perform on a feedback record is to "Approve/Publish" it.
     * This action toggles the public_visibility state, allowing that specific feedback to be viewed by other customers on the public vehicle catalog page.
     */
    @PostMapping("/{id}/toggle-visibility")
    public String toggleVisibilityPost(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return toggleVisibility(id, authentication, redirectAttributes);
    }

    @GetMapping("/{id}/toggle-visibility")
    public String toggleVisibilityGet(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return toggleVisibility(id, authentication, redirectAttributes);
    }

    @PostMapping("/{id}/approve")
    public String approvePost(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return toggleVisibility(id, authentication, redirectAttributes);
    }

    @GetMapping("/{id}/approve")
    public String approveGet(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return toggleVisibility(id, authentication, redirectAttributes);
    }

    private String toggleVisibility(Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (!isStaff(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff can toggle feedback visibility.");
        }
        Feedback updated = feedbackService.togglePublicVisibility(id);
        boolean isPublic = Boolean.TRUE.equals(updated.getPublicVisibility());
        redirectAttributes.addFlashAttribute("successMessage",
                "Feedback #" + id + " visibility updated: " + (isPublic ? "Approved & Published to public vehicle catalog." : "Hidden from public catalog."));
        return "redirect:/feedback";
    }

    @GetMapping("/{id}/resolve")
    public String resolveFeedback(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (!isStaff(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff can resolve feedback.");
        }
        feedbackService.resolveFeedback(id);
        redirectAttributes.addFlashAttribute("successMessage", "Feedback #" + id + " has been marked as resolved.");
        return "redirect:/feedback";
    }

    /**
     * Customer Permissions: Customers can delete their feedback at any time.
     */
    @GetMapping("/{id}/delete")
    public String deleteFeedbackGet(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteFeedback(id, authentication, redirectAttributes);
    }

    @PostMapping("/{id}/delete")
    public String deleteFeedbackPost(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return deleteFeedback(id, authentication, redirectAttributes);
    }

    private String deleteFeedback(Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        boolean staff = isStaff(authentication);
        if (staff) {
            feedbackService.deleteFeedback(id);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback record #" + id + " removed.");
        } else {
            String email = authentication.getName();
            Customer customer = customerRepository.findByEmail(email).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer not found."));
            try {
                feedbackService.deleteCustomerFeedback(id, customer);
                redirectAttributes.addFlashAttribute("successMessage", "Your feedback has been successfully removed.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Could not remove feedback: " + e.getMessage());
            }
        }
        return "redirect:/feedback";
    }
}
