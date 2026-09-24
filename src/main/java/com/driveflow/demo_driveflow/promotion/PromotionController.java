package com.driveflow.demo_driveflow.promotion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public String listPromotions(Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions());
        return "promotion/promotion-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("promotion", new Promotion());
        return "promotion/promotion-form";
    }

    @PostMapping
    public String createPromotion(@ModelAttribute Promotion promotion, RedirectAttributes redirectAttributes) {
        promotionService.createPromotion(promotion);
        redirectAttributes.addFlashAttribute("successMessage", "Promotion campaign '" + promotion.getTitle() + "' created successfully.");
        return "redirect:/promotions";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("promotion", promotionService.getPromotionById(id));
        return "promotion/promotion-form";
    }

    @PostMapping("/{id}")
    public String updatePromotion(@PathVariable Long id, @ModelAttribute Promotion promotion, RedirectAttributes redirectAttributes) {
        promotionService.updatePromotion(id, promotion);
        redirectAttributes.addFlashAttribute("successMessage", "Promotion campaign '" + promotion.getTitle() + "' updated successfully.");
        return "redirect:/promotions";
    }

    @GetMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.removePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion offer #PR-" + id + " removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove promotion: " + e.getMessage());
        }
        return "redirect:/promotions";
    }
}
