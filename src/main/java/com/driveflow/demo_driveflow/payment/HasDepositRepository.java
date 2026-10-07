package com.driveflow.demo_driveflow.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HasDepositRepository extends JpaRepository<HasDeposit, Long> {
    List<HasDeposit> findAllByOrderByDepositDateDesc();
    Optional<HasDeposit> findByBookingBookingId(Long bookingId);
}
