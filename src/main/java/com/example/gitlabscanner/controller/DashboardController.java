package com.example.gitlabscanner.controller;

import com.example.gitlabscanner.ScannerService;
import com.example.gitlabscanner.model.ScanResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class DashboardController {
    private final ScannerService scannerService;

    public DashboardController(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    @GetMapping
    public String index() {
        return "index";
    }

    @PostMapping("scan-user")
    public String scanUser(
            @RequestParam String username,
            @RequestParam(required = false) String token,
            Model model) {
        try {
            ScanResult result = scannerService.scanUserProjects(username, token);
            model.addAttribute("scanResult", result);
            model.addAttribute("scanType", "User: " + username);
        } catch (Exception e) {
            model.addAttribute("error", "Error scanning user: " + e.getMessage());
        }
        return "dashboard";
    }

    @PostMapping("scan-group")
    public String scanGroup(
            @RequestParam String groupName,
            @RequestParam(required = false) String token,
            Model model) {
        try {
            ScanResult result = scannerService.scanGroupProjects(groupName, token);
            model.addAttribute("scanResult", result);
            model.addAttribute("scanType", "Group: " + groupName);
        } catch (Exception e) {
            model.addAttribute("error", "Error scanning group: " + e.getMessage());
        }
        return "dashboard";
    }
}
