package com.driveflow.demo_driveflow.promotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private Promotion existingPromo;

    @BeforeEach
    void setUp() {
        existingPromo = new Promotion();
        existingPromo.setPromotionId(1L);
        existingPromo.setTitle("Summer Park Super Saver");
        existingPromo.setCouponId("SUMMER15");
        existingPromo.setCouponCode("SUMMER15");
        existingPromo.setVehicleSeason("SUMMER");
        existingPromo.setVehicleCategory("SEDAN");
        existingPromo.setDiscountRate(new BigDecimal("15.00"));
        existingPromo.setStartDate(LocalDate.of(2026, 6, 1));
        existingPromo.setEndDate(LocalDate.of(2026, 8, 31));
        existingPromo.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Modifying discount percentage of existing promotion throws 400 Bad Request ResponseStatusException")
    void updatePromotion_whenDiscountRateModified_throws400BadRequest() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromo));

        Promotion updateRequest = new Promotion();
        updateRequest.setDiscountRate(new BigDecimal("25.00"));
        updateRequest.setEndDate(LocalDate.of(2026, 9, 30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                promotionService.updatePromotion(1L, updateRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("discount percentage"));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modifying start date of existing promotion throws 400 Bad Request ResponseStatusException")
    void updatePromotion_whenStartDateModified_throws400BadRequest() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromo));

        Promotion updateRequest = new Promotion();
        updateRequest.setStartDate(LocalDate.of(2026, 5, 1));
        updateRequest.setEndDate(LocalDate.of(2026, 9, 30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                promotionService.updatePromotion(1L, updateRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("start date"));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modifying coupon ID of existing promotion throws 400 Bad Request ResponseStatusException")
    void updatePromotion_whenCouponIdModified_throws400BadRequest() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromo));

        Promotion updateRequest = new Promotion();
        updateRequest.setCouponId("NEWCODE99");
        updateRequest.setEndDate(LocalDate.of(2026, 9, 30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                promotionService.updatePromotion(1L, updateRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("coupon ID"));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating only end date succeeds and updates promotion expiration date")
    void updatePromotion_whenOnlyEndDateModified_succeeds() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(existingPromo));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate newEndDate = LocalDate.of(2026, 12, 31);
        Promotion updateRequest = new Promotion();
        updateRequest.setEndDate(newEndDate);

        Promotion result = promotionService.updatePromotion(1L, updateRequest);

        assertNotNull(result);
        assertEquals(newEndDate, result.getEndDate());
        assertEquals(new BigDecimal("15.00"), result.getDiscountRate());
        assertEquals(LocalDate.of(2026, 6, 1), result.getStartDate());
        assertEquals("SUMMER15", result.getCouponId());
        verify(promotionRepository, times(1)).save(existingPromo);
    }

    @Test
    @DisplayName("Creating promotion without couponId auto-generates a unique coupon ID")
    void createPromotion_whenCouponIdMissing_autoGenerates() {
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Promotion newPromo = new Promotion();
        newPromo.setTitle("Flash Winter Promo");
        newPromo.setDiscountRate(new BigDecimal("10.00"));
        newPromo.setStartDate(LocalDate.now());
        newPromo.setEndDate(LocalDate.now().plusDays(30));

        Promotion saved = promotionService.createPromotion(newPromo);

        assertNotNull(saved.getCouponId());
        assertTrue(saved.getCouponId().startsWith("CPN-"));
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("ALL_SEASONS", saved.getVehicleSeason());
        assertEquals("ALL", saved.getVehicleCategory());
        verify(promotionRepository, times(1)).save(newPromo);
    }

    @Test
    @DisplayName("Staff removing promotion calls repository deleteById")
    void removePromotion_deletesById() {
        doNothing().when(promotionRepository).deleteById(1L);

        promotionService.removePromotion(1L);

        verify(promotionRepository, times(1)).deleteById(1L);
    }
}
