package com.driveflow.demo_driveflow.vehicle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    public String listVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "vehicle/vehicle-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        return "vehicle/vehicle-form";
    }

    @PostMapping
    public String registerVehicle(@ModelAttribute Vehicle vehicle) {
        vehicleService.registerVehicle(vehicle);
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicleById(id));
        return "vehicle/vehicle-form";
    }

    @PostMapping("/{id}")
    public String updateVehicle(@PathVariable Long id, @ModelAttribute Vehicle vehicle) {
        vehicleService.updateVehicle(id, vehicle);
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id) {
        vehicleService.removeVehicle(id);
        return "redirect:/vehicles";
    }
}
