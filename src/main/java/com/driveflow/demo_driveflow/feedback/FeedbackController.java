package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @GetMapping
    public String listFeedback(Model model, Authentication authentication) {
        boolean isStaff = false;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            isStaff = staffRepository.findByEmail(email).isPresent();
            model.addAttribute("isStaff", isStaff);

            if (isStaff) {
                model.addAttribute("feedbackList", feedbackService.getAllFeedback());
            } else {
                Optional<Customer> customerOpt = customerRepository.findByEmail(email);
                if (customerOpt.isPresent()) {
                    model.addAttribute("feedbackList", feedbackService.getFeedbackByCustomer(customerOpt.get()));
                } else {
                    model.addAttribute("feedbackList", feedbackService.getAllFeedback());
                }
            }
        } else {
            model.addAttribute("isStaff", false);
            model.addAttribute("feedbackList", feedbackService.getAllFeedback());
        }
        return "feedback/feedback-list";
    }

    @GetMapping("/new")
    public String showSubmitForm(Model model) {
        Feedback feedback = new Feedback();
        feedback.setDate(LocalDate.now());
        feedback.setStatus("OPEN");
        model.addAttribute("feedback", feedback);
        return "feedback/feedback-form";
    }

    @PostMapping
    public String submitFeedback(@ModelAttribute Feedback feedback, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            customerRepository.findByEmail(email).ifPresent(feedback::setCustomer);
        }
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }
        if (feedback.getStatus() == null || feedback.getStatus().isBlank()) {
            feedback.setStatus("OPEN");
        }
        feedbackService.submitFeedback(feedback);
        return "redirect:/feedback";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        Feedback feedback = feedbackService.getFeedbackById(id);
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            boolean isStaff = staffRepository.findByEmail(email).isPresent();
            if (!isStaff && (feedback.getCustomer() == null || !feedback.getCustomer().getEmail().equalsIgnoreCase(email))) {
                return "redirect:/feedback?error=unauthorized";
            }
        }
        model.addAttribute("feedback", feedback);
        return "feedback/feedback-form";
    }

    @PostMapping("/{id}")
    public String updateFeedback(@PathVariable Long id, @ModelAttribute Feedback feedback, Authentication authentication, RedirectAttributes redirectAttributes) {
        Feedback existing = feedbackService.getFeedbackById(id);
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            boolean isStaff = staffRepository.findByEmail(email).isPresent();
            if (!isStaff && (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email))) {
                return "redirect:/feedback?error=unauthorized";
            }
        }
        feedbackService.updateFeedback(id, feedback);
        redirectAttributes.addFlashAttribute("successMessage", "Feedback ticket #FB-" + id + " updated successfully.");
        return "redirect:/feedback";
    }

    @GetMapping("/{id}/resolve")
    public String resolveFeedback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        feedbackService.resolveFeedback(id);
        redirectAttributes.addFlashAttribute("successMessage", "Feedback ticket #FB-" + id + " has been marked as resolved.");
        return "redirect:/feedback";
    }

    @GetMapping("/{id}/delete")
    public String deleteFeedback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.deleteFeedback(id);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback ticket #FB-" + id + " removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove feedback ticket: " + e.getMessage());
        }
        return "redirect:/feedback";
    }
}
