package com.driveflow.demo_driveflow.branch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/branches")
public class BranchController {

    @Autowired
    private BranchRepository branchRepository;

    @GetMapping
    public String listBranches(Model model) {
        model.addAttribute("branches", branchRepository.findAll());
        return "branch/branch-list";
    }

    @GetMapping("/new")
    public String showCreateBranchForm(Model model) {
        model.addAttribute("branch", new Branch());
        return "branch/branch-form";
    }

    @PostMapping
    public String createBranch(@ModelAttribute Branch branch,
                               @RequestParam(value = "contactNumbers", required = false) List<String> contactNumbers,
                               RedirectAttributes redirectAttributes) {
        if (branch.getBranchName() == null || branch.getBranchName().trim().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Branch name is required.");
            return "redirect:/branches/new";
        }

        List<String> cleanList = new ArrayList<>();
        if (branch.getContactNumber() != null && !branch.getContactNumber().trim().isBlank()) {
            cleanList.add(branch.getContactNumber().trim());
        }
        if (contactNumbers != null) {
            for (String cn : contactNumbers) {
                if (cn != null && !cn.trim().isBlank()) {
                    String clean = cn.trim();
                    if (!cleanList.contains(clean)) {
                        cleanList.add(clean);
                    }
                }
            }
        }
        if (!cleanList.isEmpty()) {
            branch.setContactNumber(cleanList.get(0));
            branch.setContactNumbers(cleanList);
        }

        branchRepository.save(branch);
        redirectAttributes.addFlashAttribute("successMessage",
                "Branch '" + branch.getBranchName() + "' registered successfully with " + branch.getContactNumbers().size() + " contact number(s).");
        return "redirect:/branches";
    }

    @GetMapping("/{id}/edit")
    public String showEditBranchForm(@PathVariable Long id, Model model) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
        model.addAttribute("branch", branch);
        return "branch/branch-form";
    }

    @PostMapping("/{id}")
    public String updateBranch(@PathVariable Long id,
                               @ModelAttribute Branch formBranch,
                               @RequestParam(value = "contactNumbers", required = false) List<String> contactNumbers,
                               RedirectAttributes redirectAttributes) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));

        branch.setBranchName(formBranch.getBranchName());
        branch.setStreet(formBranch.getStreet());
        branch.setCity(formBranch.getCity());
        branch.setEmail(formBranch.getEmail());

        List<String> cleanList = new ArrayList<>();
        if (formBranch.getContactNumber() != null && !formBranch.getContactNumber().trim().isBlank()) {
            cleanList.add(formBranch.getContactNumber().trim());
        }
        if (contactNumbers != null) {
            for (String cn : contactNumbers) {
                if (cn != null && !cn.trim().isBlank()) {
                    String clean = cn.trim();
                    if (!cleanList.contains(clean)) {
                        cleanList.add(clean);
                    }
                }
            }
        }
        if (!cleanList.isEmpty()) {
            branch.setContactNumber(cleanList.get(0));
            branch.setContactNumbers(cleanList);
        }

        branchRepository.save(branch);
        redirectAttributes.addFlashAttribute("successMessage",
                "Branch '" + branch.getBranchName() + "' updated successfully.");
        return "redirect:/branches";
    }

    @PostMapping("/{id}/delete")
    public String deleteBranch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            branchRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Branch successfully removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete branch: " + ex.getMessage());
        }
        return "redirect:/branches";
    }
}
