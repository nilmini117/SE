package com.driveflow.demo_driveflow.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByStatusIgnoreCase(String status);

    @Query("SELECT p FROM Promotion p WHERE UPPER(p.status) = 'ACTIVE' " +
           "AND (p.startDate IS NULL OR p.startDate <= :today) " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<Promotion> findActivePromotions(@Param("today") LocalDate today);

    @Query("SELECT p FROM Promotion p WHERE UPPER(p.status) = 'ACTIVE' " +
           "AND (p.vehicle IS NULL OR p.vehicle.vehicleId = :vehicleId) " +
           "AND (p.startDate IS NULL OR p.startDate <= :today) " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<Promotion> findActivePromotionsForVehicle(@Param("vehicleId") Long vehicleId, @Param("today") LocalDate today);

    Optional<Promotion> findByCouponCodeIgnoreCaseAndStatusIgnoreCase(String couponCode, String status);

    @Query("SELECT p FROM Promotion p WHERE UPPER(p.status) = 'ACTIVE' " +
           "AND (LOWER(p.couponCode) = LOWER(:code) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :code, '%')))")
    List<Promotion> findMatchingPromotions(@Param("code") String code);
}
