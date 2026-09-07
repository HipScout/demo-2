package com.example.gitlabscanner.controller;

import com.example.gitlabscanner.ScannerService;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.report.ReportGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class DashboardController {
    private final ScannerService scannerService;
    private final ReportGenerator reportGenerator;

    public DashboardController(ScannerService scannerService, ReportGenerator reportGenerator) {
        this.scannerService = scannerService;
        this.reportGenerator = reportGenerator;
    }

    @GetMapping
    public String index() {
        return "index";
    }

    @RequestMapping(value = "scan-user", method = {RequestMethod.GET, RequestMethod.POST})
    public String scanUser(
            @RequestParam String username,
            @RequestParam(required = false) String token,
            Model model) {
        try {
            ScanResult result = scannerService.scanUserProjects(username, token);
            model.addAttribute("scanResult", result);
            model.addAttribute("scanType", "User: " + username);
            model.addAttribute("targetType", "user");
            model.addAttribute("targetName", username);
            model.addAttribute("scanToken", token != null ? token : "");
        } catch (Exception e) {
            model.addAttribute("error", "Error scanning user: " + e.getMessage());
        }
        return "dashboard";
    }

    @RequestMapping(value = "scan-group", method = {RequestMethod.GET, RequestMethod.POST})
    public String scanGroup(
            @RequestParam String groupName,
            @RequestParam(required = false) String token,
            Model model) {
        try {
            ScanResult result = scannerService.scanGroupProjects(groupName, token);
            model.addAttribute("scanResult", result);
            model.addAttribute("scanType", "Group: " + groupName);
            model.addAttribute("targetType", "group");
            model.addAttribute("targetName", groupName);
            model.addAttribute("scanToken", token != null ? token : "");
        } catch (Exception e) {
            model.addAttribute("error", "Error scanning group: " + e.getMessage());
        }
        return "dashboard";
    }

    @GetMapping(value = "download/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam String type,
            @RequestParam String target,
            @RequestParam(required = false) String token) {
        ScanResult result = "user".equalsIgnoreCase(type) ?
                scannerService.scanUserProjects(target, token) :
                scannerService.scanGroupProjects(target, token);
        byte[] pdf = reportGenerator.generatePdfReport(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan-" + target + ".pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "download/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> downloadJson(
            @RequestParam String type,
            @RequestParam String target,
            @RequestParam(required = false) String token) {
        ScanResult result = "user".equalsIgnoreCase(type) ?
                scannerService.scanUserProjects(target, token) :
                scannerService.scanGroupProjects(target, token);
        String json = reportGenerator.generateJsonReport(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan-" + target + ".json\"")
                .body(json);
    }
}
