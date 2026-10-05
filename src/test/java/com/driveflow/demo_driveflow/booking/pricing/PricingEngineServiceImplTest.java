package com.driveflow.demo_driveflow.booking.pricing;

import com.driveflow.demo_driveflow.promotion.Promotion;
import com.driveflow.demo_driveflow.promotion.PromotionRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PricingEngineServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private PricingEngineServiceImpl pricingEngineService;

    private Vehicle testVehicle;
    private Promotion summerPromo;

    @BeforeEach
    void setUp() {
        testVehicle = new Vehicle();
        testVehicle.setVehicleId(101L);
        testVehicle.setModel("Toyota Prius Prime");
        testVehicle.setDailyRate(new BigDecimal("12500.00"));

        summerPromo = new Promotion();
        summerPromo.setPromotionId(1L);
        summerPromo.setTitle("Summer Park Super Saver");
        summerPromo.setCouponId("SUMMER15");
        summerPromo.setCouponCode("SUMMER15");
        summerPromo.setDiscountRate(new BigDecimal("15.00"));
        summerPromo.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should apply valid coupon code and calculate discounted total accurately")
    void testCalculatePricing_WhenValidCouponEntered_AppliesDiscount() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(testVehicle));
        when(promotionRepository.findByCouponIdIgnoreCaseAndStatusIgnoreCase("SUMMER15", "ACTIVE"))
                .thenReturn(Optional.of(summerPromo));

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(3); // 3 days

        PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                101L, 1L, start, end, "SUMMER15"
        );

        assertNotNull(breakdown);
        assertTrue(breakdown.isPromotionApplied());
        assertTrue(breakdown.isCouponValid());
        assertEquals("SUMMER15", breakdown.getCouponId());
        assertEquals(new BigDecimal("15.00"), breakdown.getDiscountRate());

        // 3 days * 12500 = 37500.00
        assertEquals(new BigDecimal("37500.00"), breakdown.getBaseCost());
        // 15% discount of 37500 = 5625.00
        assertEquals(new BigDecimal("5625.00"), breakdown.getDiscountAmount());
        // Final total = 37500 - 5625 = 31875.00
        assertEquals(new BigDecimal("31875.00"), breakdown.getFinalTotalCost());
        assertTrue(breakdown.getCouponMessage().contains("SUMMER15"));
    }

    @Test
    @DisplayName("Should handle case-insensitive coupon codes e.g. 'summer15'")
    void testCalculatePricing_WhenCouponCodeLowerCase_MatchesCaseInsensitively() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(testVehicle));
        when(promotionRepository.findByCouponIdIgnoreCaseAndStatusIgnoreCase("summer15", "ACTIVE"))
                .thenReturn(Optional.of(summerPromo));

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2); // 2 days

        PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                101L, 1L, start, end, "  summer15  "
        );

        assertTrue(breakdown.isCouponValid());
        assertTrue(breakdown.isPromotionApplied());
        assertEquals("SUMMER15", breakdown.getCouponId());
        assertEquals(new BigDecimal("25000.00"), breakdown.getBaseCost());
        assertEquals(new BigDecimal("3750.00"), breakdown.getDiscountAmount());
        assertEquals(new BigDecimal("21250.00"), breakdown.getFinalTotalCost());
    }

    @Test
    @DisplayName("Should flag invalid coupon code without breaking rate calculation")
    void testCalculatePricing_WhenInvalidCouponEntered_ReturnsCouponInvalidNotice() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(testVehicle));
        when(promotionRepository.findByCouponIdIgnoreCaseAndStatusIgnoreCase("FAKECODE", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(promotionRepository.findByCouponCodeIgnoreCaseAndStatusIgnoreCase("FAKECODE", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(promotionRepository.findByCouponIdIgnoreCase("FAKECODE"))
                .thenReturn(Optional.empty());
        when(promotionRepository.findByCouponCodeIgnoreCase("FAKECODE"))
                .thenReturn(Optional.empty());
        when(promotionRepository.findMatchingPromotions("FAKECODE"))
                .thenReturn(List.of());

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(3);

        PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                101L, 1L, start, end, "FAKECODE"
        );

        assertNotNull(breakdown);
        assertFalse(breakdown.isCouponValid());
        assertFalse(breakdown.isPromotionApplied());
        assertTrue(breakdown.getCouponMessage().contains("Invalid coupon code 'FAKECODE'"));
        assertEquals(new BigDecimal("37500.00"), breakdown.getFinalTotalCost());
    }

    @Test
    @DisplayName("Should apply coupon targeted at ALL to any vehicle category without error")
    void testCalculatePricing_WhenPromotionCategoryIsAll_AppliesToAnyVehicleCategory() {
        // Vehicle is an SUV
        Vehicle suvVehicle = new Vehicle();
        suvVehicle.setVehicleId(202L);
        suvVehicle.setModel("Toyota Land Cruiser Prado");
        suvVehicle.setCategory("SUV");
        suvVehicle.setDailyRate(new BigDecimal("20000.00"));

        Promotion universalPromo = new Promotion();
        universalPromo.setPromotionId(6L);
        universalPromo.setTitle("Summer Trip");
        universalPromo.setCouponId("SUMMER12");
        universalPromo.setCouponCode("SUMMER12");
        universalPromo.setVehicleCategory("ALL"); // Targeted at ALL
        universalPromo.setDiscountRate(new BigDecimal("12.00"));
        universalPromo.setStatus("ACTIVE");

        when(vehicleRepository.findById(202L)).thenReturn(Optional.of(suvVehicle));
        when(promotionRepository.findByCouponIdIgnoreCaseAndStatusIgnoreCase("SUMMER12", "ACTIVE"))
                .thenReturn(Optional.of(universalPromo));

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2); // 2 days

        PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                202L, 1L, start, end, "SUMMER12"
        );

        assertNotNull(breakdown);
        assertTrue(breakdown.isCouponValid());
        assertTrue(breakdown.isPromotionApplied());
        assertEquals("SUMMER12", breakdown.getCouponId());
        // Base cost: 2 days * 20000 = 40000.00
        assertEquals(new BigDecimal("40000.00"), breakdown.getBaseCost());
        // 12% of 40000 = 4800.00
        assertEquals(new BigDecimal("4800.00"), breakdown.getDiscountAmount());
        // Final total: 40000 - 4800 = 35200.00
        assertEquals(new BigDecimal("35200.00"), breakdown.getFinalTotalCost());
    }

    @Test
    @DisplayName("Should reject coupon when category does not match vehicle category and is not ALL")
    void testCalculatePricing_WhenPromotionCategoryDiffersFromVehicle_RejectsCoupon() {
        // Vehicle is a Sedan
        Vehicle sedanVehicle = new Vehicle();
        sedanVehicle.setVehicleId(303L);
        sedanVehicle.setModel("Toyota Corolla Axio");
        sedanVehicle.setCategory("SEDAN");
        sedanVehicle.setDailyRate(new BigDecimal("10000.00"));

        // Promotion strictly for ELECTRIC
        Promotion evPromo = new Promotion();
        evPromo.setPromotionId(7L);
        evPromo.setTitle("Website Special EV");
        evPromo.setCouponId("WEB0");
        evPromo.setCouponCode("WEB0");
        evPromo.setVehicleCategory("ELECTRIC");
        evPromo.setDiscountRate(new BigDecimal("20.00"));
        evPromo.setStatus("ACTIVE");

        when(vehicleRepository.findById(303L)).thenReturn(Optional.of(sedanVehicle));
        when(promotionRepository.findByCouponIdIgnoreCaseAndStatusIgnoreCase("WEB0", "ACTIVE"))
                .thenReturn(Optional.of(evPromo));

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(1);

        PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                303L, 1L, start, end, "WEB0"
        );

        assertNotNull(breakdown);
        assertFalse(breakdown.isCouponValid());
        assertFalse(breakdown.isPromotionApplied());
        assertTrue(breakdown.getCouponMessage().contains("not valid for this vehicle category"));
        assertEquals(new BigDecimal("10000.00"), breakdown.getFinalTotalCost());
    }

    @Test
    @DisplayName("Direct validateCouponForVehicle enforces ALL compatibility and throws for mismatch")
    void testValidateCouponForVehicle_DirectValidation() {
        Vehicle suv = new Vehicle();
        suv.setModel("Toyota Prado");
        suv.setCategory("SUV");

        Promotion allPromo = new Promotion();
        allPromo.setVehicleCategory("ALL");

        Promotion suvPromo = new Promotion();
        suvPromo.setVehicleCategory("SUV");

        Promotion evPromo = new Promotion();
        evPromo.setVehicleCategory("ELECTRIC");

        // "ALL" promo on SUV: does not throw
        assertDoesNotThrow(() -> pricingEngineService.validateCouponForVehicle(allPromo, suv));

        // "SUV" promo on SUV: does not throw
        assertDoesNotThrow(() -> pricingEngineService.validateCouponForVehicle(suvPromo, suv));

        // "ELECTRIC" promo on SUV: throws RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                pricingEngineService.validateCouponForVehicle(evPromo, suv)
        );
        assertEquals("This coupon is not valid for this vehicle category.", ex.getMessage());
    }
}
