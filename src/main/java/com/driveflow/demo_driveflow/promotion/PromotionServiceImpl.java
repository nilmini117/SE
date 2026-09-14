package com.driveflow.demo_driveflow.promotion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Override
    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));
    }

    @Override
    public Promotion createPromotion(Promotion promotion) {
        promotion.setStatus("ACTIVE");
        return promotionRepository.save(promotion);
    }

    @Override
    public Promotion updatePromotion(Long id, Promotion updatedPromotion) {
        Promotion existing = getPromotionById(id);
        existing.setTitle(updatedPromotion.getTitle());
        existing.setDiscountRate(updatedPromotion.getDiscountRate());
        existing.setStartDate(updatedPromotion.getStartDate());
        existing.setEndDate(updatedPromotion.getEndDate());
        existing.setStatus(updatedPromotion.getStatus());
        return promotionRepository.save(existing);
    }

    @Override
    public void removePromotion(Long id) {
        promotionRepository.deleteById(id);
    }
}
