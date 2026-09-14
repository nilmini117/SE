package com.driveflow.demo_driveflow.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public String listPayments(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "payment/payment-list";
    }

    @GetMapping("/new")
    public String showPaymentForm(Model model) {
        model.addAttribute("payment", new Payment());
        return "payment/payment-form";
    }

    @PostMapping
    public String processPayment(@ModelAttribute Payment payment) {
        paymentService.processPayment(payment);
        return "redirect:/payments";
    }

    @GetMapping("/{id}/cancel")
    public String cancelPayment(@PathVariable Long id) {
        paymentService.cancelPayment(id);
        return "redirect:/payments";
    }

    @GetMapping("/invoices")
    public String listInvoices(Model model) {
        model.addAttribute("invoices", paymentService.getAllInvoices());
        return "payment/invoice-list";
    }

    @GetMapping("/invoices/new")
    public String showInvoiceForm(Model model) {
        model.addAttribute("invoice", new Invoice());
        return "payment/invoice-form";
    }

    @PostMapping("/invoices")
    public String generateInvoice(@ModelAttribute Invoice invoice) {
        paymentService.generateInvoice(invoice);
        return "redirect:/payments/invoices";
    }
}
