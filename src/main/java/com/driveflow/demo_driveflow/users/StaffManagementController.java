package com.driveflow.demo_driveflow.users;

import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/staff")
public class StaffManagementController {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String listEmployees(Model model) {
        List<Staff> staffMembers = staffRepository.findAll();
        List<Branch> branches = branchRepository.findAll();

        long activeCount = staffMembers.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .count();
        long suspendedCount = staffMembers.size() - activeCount;

        BigDecimal totalPayroll = staffMembers.stream()
                .map(s -> s.getSalary() != null ? s.getSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("employees", staffMembers);
        model.addAttribute("branches", branches);
        model.addAttribute("totalStaff", staffMembers.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("suspendedCount", suspendedCount);
        model.addAttribute("totalPayroll", totalPayroll);

        return "staff/employee-management";
    }

    @PostMapping
    public String registerStaff(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("nic") String nic,
            @RequestParam(value = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(value = "salary", required = false) BigDecimal salary,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam("password") String password,
            @RequestParam(value = "contactNumber", required = false) String primaryContact,
            @RequestParam(value = "contactNumbers", required = false) List<String> contactNumbers,
            RedirectAttributes redirectAttributes) {

        try {
            if (email == null || email.trim().isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email address is required.");
                return "redirect:/staff";
            }
            String cleanEmail = email.trim().toLowerCase();
            if (userRepository.findByEmail(cleanEmail).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "An account with email '" + cleanEmail + "' already exists.");
                return "redirect:/staff";
            }

            Staff staff = new Staff();
            staff.setFirstName(firstName != null ? firstName.trim() : "");
            staff.setLastName(lastName != null ? lastName.trim() : "");
            staff.setEmail(cleanEmail);
            staff.setNic(nic != null ? nic.trim() : "");
            staff.setDob(dob);
            staff.setSalary(salary != null ? salary : BigDecimal.ZERO);
            staff.setPassword(passwordEncoder.encode(password));
            staff.setIsActive(true);

            if (branchId != null) {
                branchRepository.findById(branchId).ifPresent(staff::setBranch);
            }

            List<String> cleanContacts = new ArrayList<>();
            if (primaryContact != null && !primaryContact.trim().isBlank()) {
                cleanContacts.add(primaryContact.trim());
            }
            if (contactNumbers != null) {
                for (String cn : contactNumbers) {
                    if (cn != null && !cn.trim().isBlank()) {
                        String clean = cn.trim();
                        if (!cleanContacts.contains(clean)) {
                            cleanContacts.add(clean);
                        }
                    }
                }
            }
            if (!cleanContacts.isEmpty()) {
                staff.setContactNumber(cleanContacts.get(0));
                staff.setContactNumbers(cleanContacts);
            }

            staffRepository.save(staff);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Staff employee '" + staff.getName() + "' successfully registered with system access.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to register staff: " + e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/{id}/edit")
    public String updateStaff(
            @PathVariable Long id,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("nic") String nic,
            @RequestParam(value = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(value = "salary", required = false) BigDecimal salary,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "contactNumber", required = false) String primaryContact,
            @RequestParam(value = "contactNumbers", required = false) List<String> contactNumbers,
            RedirectAttributes redirectAttributes) {

        try {
            Staff staff = staffRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + id));

            staff.setFirstName(firstName != null ? firstName.trim() : "");
            staff.setLastName(lastName != null ? lastName.trim() : "");
            staff.setNic(nic != null ? nic.trim() : "");
            staff.setDob(dob);
            staff.setSalary(salary != null ? salary : BigDecimal.ZERO);

            if (branchId != null) {
                branchRepository.findById(branchId).ifPresent(staff::setBranch);
            } else {
                staff.setBranch(null);
            }

            if (newPassword != null && !newPassword.trim().isBlank()) {
                staff.setPassword(passwordEncoder.encode(newPassword.trim()));
            }

            List<String> cleanContacts = new ArrayList<>();
            if (primaryContact != null && !primaryContact.trim().isBlank()) {
                cleanContacts.add(primaryContact.trim());
            }
            if (contactNumbers != null) {
                for (String cn : contactNumbers) {
                    if (cn != null && !cn.trim().isBlank()) {
                        String clean = cn.trim();
                        if (!cleanContacts.contains(clean)) {
                            cleanContacts.add(clean);
                        }
                    }
                }
            }
            if (!cleanContacts.isEmpty()) {
                staff.setContactNumber(cleanContacts.get(0));
                staff.setContactNumbers(cleanContacts);
            }

            staffRepository.save(staff);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Details updated successfully for staff employee '" + staff.getName() + "'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update staff: " + e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/{id}/toggle-access")
    public String toggleSystemAccess(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Staff staff = staffRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + id));

            boolean current = staff.getIsActive() == null || staff.getIsActive();
            staff.setIsActive(!current);
            staffRepository.save(staff);

            String status = staff.getIsActive() ? "ENABLED" : "SUSPENDED";
            redirectAttributes.addFlashAttribute("successMessage",
                    "System access for '" + staff.getName() + "' is now " + status + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update access: " + e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/{id}/delete")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            staffRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Staff account removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete staff account: " + e.getMessage());
        }
        return "redirect:/staff";
    }
}
