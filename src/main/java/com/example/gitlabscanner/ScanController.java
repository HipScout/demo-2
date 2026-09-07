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
    @Operation(summary = "Scan public repositories for a GitLab user")
    @ResponseBody
    public ScanResult scanUser(
            @Parameter(description = "GitLab username")
            @RequestParam String username,
            @Parameter(description = "Optional GitLab personal access token for private repos")
            @RequestParam(required = false) String token) {
        return scannerService.scanUserProjects(username, token);
    }

    @PostMapping("/scan/group")
    @Operation(summary = "Scan public repositories for a GitLab group")
    @ResponseBody
    public ScanResult scanGroup(
            @Parameter(description = "GitLab group name")
            @RequestParam String groupName,
            @Parameter(description = "Optional GitLab personal access token")
            @RequestParam(required = false) String token) {
        return scannerService.scanGroupProjects(groupName, token);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    @ResponseBody
    public String health() {
        return "GitLab Scanner is running";
    }
}
