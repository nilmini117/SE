package com.driveflow.demo_driveflow.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/payments")
public class RefundController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/refunds")
    public String listRefunds(Model model) {
        model.addAttribute("refunds", paymentService.getAllRefunds());
        return "payment/refund-list";
    }

    @GetMapping("/{paymentId}/refund")
    public String showRefundForm(@PathVariable Long paymentId, Model model) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(payment.getAmountPaid());
        refund.setApprovalStatus("PENDING");
        model.addAttribute("refund", refund);
        model.addAttribute("payment", payment);
        return "payment/refund-form";
    }

    @PostMapping("/{paymentId}/refund")
    public String issueRefund(@PathVariable Long paymentId,
                              @RequestParam("amount") BigDecimal amount) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(amount != null ? amount : payment.getAmountPaid());
        refund.setApprovalStatus("PENDING");
        paymentService.issueRefund(refund);
        return "redirect:/payments/refunds";
    }

    @GetMapping("/refunds/{id}/approve")
    public String approveRefund(@PathVariable Long id) {
        paymentService.approveRefund(id);
        return "redirect:/payments/refunds";
    }

    @GetMapping("/refunds/{id}/reject")
    public String rejectRefund(@PathVariable Long id) {
        paymentService.rejectRefund(id);
        return "redirect:/payments/refunds";
    }
}
