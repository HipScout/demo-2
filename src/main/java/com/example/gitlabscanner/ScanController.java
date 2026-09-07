package com.example.gitlabscanner;

import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.report.ReportGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
@Tag(name = "GitLab Scanner", description = "APIs for scanning GitLab repositories for security risks")
public class ScanController {
    private final ScannerService scannerService;
    private final ReportGenerator reportGenerator;

    public ScanController(ScannerService scannerService, ReportGenerator reportGenerator) {
        this.scannerService = scannerService;
        this.reportGenerator = reportGenerator;
    }

    @PostMapping("/scan/user")
    @Operation(summary = "Scan repositories for a GitLab user")
    @ResponseBody
    public ScanResult scanUser(
            @Parameter(description = "GitLab username")
            @RequestParam String username,
            @Parameter(description = "Optional GitLab personal access token")
            @RequestParam(required = false) String token) {
        return scannerService.scanUserProjects(username, token);
    }

    @PostMapping("/scan/group")
    @Operation(summary = "Scan repositories for a GitLab group")
    @ResponseBody
    public ScanResult scanGroup(
            @Parameter(description = "GitLab group name")
            @RequestParam String groupName,
            @Parameter(description = "Optional GitLab personal access token")
            @RequestParam(required = false) String token) {
        return scannerService.scanGroupProjects(groupName, token);
    }

    @GetMapping(value = "/scan/user/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Export GitLab user scan report as PDF")
    public org.springframework.http.ResponseEntity<byte[]> exportUserPdf(
            @RequestParam String username,
            @RequestParam(required = false) String token) {
        ScanResult result = scannerService.scanUserProjects(username, token);
        byte[] pdfBytes = reportGenerator.generatePdfReport(result);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan-" + username + ".pdf\"")
            .body(pdfBytes);
    }

    @GetMapping(value = "/scan/group/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Export GitLab group scan report as PDF")
    public org.springframework.http.ResponseEntity<byte[]> exportGroupPdf(
            @RequestParam String groupName,
            @RequestParam(required = false) String token) {
        ScanResult result = scannerService.scanGroupProjects(groupName, token);
        byte[] pdfBytes = reportGenerator.generatePdfReport(result);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan-" + groupName + ".pdf\"")
            .body(pdfBytes);
    }

    @GetMapping(value = "/scan/cli", produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get text/CLI table report for user or group")
    @ResponseBody
    public String getCliReport(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String token) {
        ScanResult result;
        if (username != null && !username.isBlank()) {
            result = scannerService.scanUserProjects(username, token);
        } else if (groupName != null && !groupName.isBlank()) {
            result = scannerService.scanGroupProjects(groupName, token);
        } else {
            return "Please specify either username or groupName parameter.";
        }
        return reportGenerator.generateDetailedCliReport(result);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    @ResponseBody
    public String health() {
        return "GitLab Scanner is running";
    }
}
