package com.example.gitlabscanner.cli;

import com.example.gitlabscanner.ScannerService;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.report.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Component
public class CliScannerRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(CliScannerRunner.class);

    private final ScannerService scannerService;
    private final ReportGenerator reportGenerator;

    public CliScannerRunner(ScannerService scannerService, ReportGenerator reportGenerator) {
        this.scannerService = scannerService;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = getOption(args, "scan.user", "user");
        String groupName = getOption(args, "scan.group", "group");
        String token = getOption(args, "scan.token", "token");

        if (username == null && groupName == null) {
            logger.info("GitLab Risk Scanner ready in Web Mode at http://localhost:8080 (Swagger: http://localhost:8080/swagger-ui.html)");
            return;
        }

        System.out.println("Starting GitLab Risk Scanner CLI execution...");
        ScanResult result;
        if (username != null) {
            System.out.println("Scanning public repositories for user: " + username);
            result = scannerService.scanUserProjects(username, token);
        } else {
            System.out.println("Scanning public repositories for group: " + groupName);
            result = scannerService.scanGroupProjects(groupName, token);
        }

        // Print CLI table and detailed findings to console
        String cliReport = reportGenerator.generateDetailedCliReport(result);
        System.out.println(cliReport);

        // Export JSON if requested
        String jsonExportPath = getOption(args, "export.json", "json");
        if (jsonExportPath != null && !jsonExportPath.isBlank()) {
            try {
                String json = reportGenerator.generateJsonReport(result);
                Files.writeString(Paths.get(jsonExportPath), json);
                System.out.println("Exported JSON report to: " + jsonExportPath);
            } catch (Exception e) {
                System.err.println("Failed to export JSON report: " + e.getMessage());
            }
        }

        // Export PDF if requested
        String pdfExportPath = getOption(args, "export.pdf", "pdf");
        if (pdfExportPath != null && !pdfExportPath.isBlank()) {
            try {
                byte[] pdfBytes = reportGenerator.generatePdfReport(result);
                try (FileOutputStream fos = new FileOutputStream(pdfExportPath)) {
                    fos.write(pdfBytes);
                }
                System.out.println("Exported PDF report to: " + pdfExportPath);
            } catch (Exception e) {
                System.err.println("Failed to export PDF report: " + e.getMessage());
            }
        }

        if (args.containsOption("cli.exit") || args.containsOption("cli")) {
            System.out.println("CLI scan complete. Exiting.");
            System.exit(0);
        }
    }

    private String getOption(ApplicationArguments args, String... names) {
        for (String name : names) {
            if (args.containsOption(name)) {
                List<String> values = args.getOptionValues(name);
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
                return "";
            }
        }
        return null;
    }
}
