package com.driveflow.demo_driveflow.promotion;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public String listPromotions(Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions());
        model.addAttribute("activeTab", "promotions");
        return "promotion/promotion-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Promotion promo = new Promotion();
        promo.setVehicleSeason("ALL_SEASONS");
        promo.setVehicleCategory("ALL");
        model.addAttribute("promotion", promo);
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
    public Object updatePromotion(
            @PathVariable Long id,
            @ModelAttribute Promotion promotion,
            @RequestBody(required = false) Map<String, Object> jsonBody,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Promotion targetUpdate = extractUpdateData(promotion, jsonBody);
        Promotion updated = promotionService.updatePromotion(id, targetUpdate);

        String contentType = request != null ? request.getContentType() : null;
        if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return ResponseEntity.ok(updated);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Promotion campaign '" + updated.getTitle() + "' updated successfully.");
        return "redirect:/promotions";
    }

    @PutMapping(value = "/{id}")
    @ResponseBody
    public ResponseEntity<?> updatePromotionPut(
            @PathVariable Long id,
            @ModelAttribute Promotion promotion,
            @RequestBody(required = false) Map<String, Object> jsonBody) {

        Promotion targetUpdate = extractUpdateData(promotion, jsonBody);
        Promotion updated = promotionService.updatePromotion(id, targetUpdate);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping(value = "/{id}")
    @ResponseBody
    public ResponseEntity<?> updatePromotionPatch(
            @PathVariable Long id,
            @ModelAttribute Promotion promotion,
            @RequestBody(required = false) Map<String, Object> jsonBody) {

        Promotion targetUpdate = extractUpdateData(promotion, jsonBody);
        Promotion updated = promotionService.updatePromotion(id, targetUpdate);
        return ResponseEntity.ok(updated);
    }

    private Promotion extractUpdateData(Promotion formPromo, Map<String, Object> jsonBody) {
        Promotion target = new Promotion();
        if (jsonBody != null && !jsonBody.isEmpty()) {
            if (jsonBody.containsKey("discountRate") && jsonBody.get("discountRate") != null) {
                target.setDiscountRate(new BigDecimal(jsonBody.get("discountRate").toString()));
            }
            if (jsonBody.containsKey("startDate") && jsonBody.get("startDate") != null) {
                target.setStartDate(LocalDate.parse(jsonBody.get("startDate").toString()));
            }
            if (jsonBody.containsKey("endDate") && jsonBody.get("endDate") != null) {
                target.setEndDate(LocalDate.parse(jsonBody.get("endDate").toString()));
            }
            if (jsonBody.containsKey("couponId") && jsonBody.get("couponId") != null) {
                target.setCouponId(jsonBody.get("couponId").toString());
            }
            if (jsonBody.containsKey("title") && jsonBody.get("title") != null) {
                target.setTitle(jsonBody.get("title").toString());
            }
            if (jsonBody.containsKey("status") && jsonBody.get("status") != null) {
                target.setStatus(jsonBody.get("status").toString());
            }
        } else if (formPromo != null) {
            target = formPromo;
        }
        return target;
    }

    @PostMapping("/{id}/delete")
    public String deletePromotionPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deletePromotion(id, redirectAttributes);
    }

    @GetMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.removePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion offer #PR-" + id + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove promotion: " + e.getMessage());
        }
        return "redirect:/promotions";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deletePromotionRest(@PathVariable Long id) {
        promotionService.removePromotion(id);
        return ResponseEntity.ok(Map.of("message", "Promotion #" + id + " deleted successfully."));
    }
}
