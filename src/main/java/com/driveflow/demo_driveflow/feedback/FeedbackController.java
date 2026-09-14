package com.driveflow.demo_driveflow.feedback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public String listFeedback(Model model) {
        model.addAttribute("feedbackList", feedbackService.getAllFeedback());
        return "feedback/feedback-list";
    }

    @GetMapping("/new")
    public String showSubmitForm(Model model) {
        model.addAttribute("feedback", new Feedback());
        return "feedback/feedback-form";
    }

    @PostMapping
    public String submitFeedback(@ModelAttribute Feedback feedback) {
        feedbackService.submitFeedback(feedback);
        return "redirect:/feedback";
    }

    @GetMapping("/{id}/resolve")
    public String resolveFeedback(@PathVariable Long id) {
        feedbackService.resolveFeedback(id);
        return "redirect:/feedback";
    }

    @GetMapping("/{id}/delete")
    public String deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return "redirect:/feedback";
    }
}
