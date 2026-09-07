package com.example.gitlabscanner;

import com.example.gitlabscanner.model.Project;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.model.SeverityLevel;
import com.example.gitlabscanner.report.ReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGeneratorTest {

    private ReportGenerator reportGenerator;
    private ScanResult sampleResult;

    @BeforeEach
    public void setUp() {
        reportGenerator = new ReportGenerator();

        sampleResult = new ScanResult();
        sampleResult.setUsername("test-developer");
        sampleResult.setScanTime(LocalDateTime.now());
        sampleResult.setTotalProjects(2);
        sampleResult.setProjectsWithRisks(1);

        Project p1 = new Project();
        p1.setId(101L);
        p1.setName("demo-auth-service");
        p1.setWebUrl("https://gitlab.com/test-developer/demo-auth-service");
        p1.setIsPublic(true);
        p1.setRisks(List.of(
            new Risk(RiskCategory.SENSITIVE_FILES, "Sensitive file found: .env", SeverityLevel.HIGH, ".env"),
            new Risk(RiskCategory.MISSING_METADATA, "Missing LICENSE file", SeverityLevel.LOW, "Repository root")
        ));

        Project p2 = new Project();
        p2.setId(102L);
        p2.setName("clean-frontend");
        p2.setWebUrl("https://gitlab.com/test-developer/clean-frontend");
        p2.setIsPublic(true);

        sampleResult.setProjectsScanned(List.of(p1, p2));
        sampleResult.setHighestSeverity(SeverityLevel.HIGH);
    }

    @Test
    public void testCliReportContainsExpectedStructure() {
        String report = reportGenerator.generateCliReport(sampleResult);

        assertNotNull(report);
        assertTrue(report.contains("GitLab Repository Risk Scan Report"));
        assertTrue(report.contains("test-developer"));
        assertTrue(report.contains("demo-auth-service"));
        assertTrue(report.contains("HIGH"));
    }

    @Test
    public void testJsonReportSerializesProperly() {
        String json = reportGenerator.generateJsonReport(sampleResult);

        assertNotNull(json);
        assertTrue(json.contains("\"username\": \"test-developer\""));
        assertTrue(json.contains("\"demo-auth-service\""));
        assertTrue(json.contains("SENSITIVE_FILES"));
    }

    @Test
    public void testPdfReportGeneratesValidPdfBytes() {
        byte[] pdfBytes = reportGenerator.generatePdfReport(sampleResult);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF document should have non-trivial length");
        // Check standard PDF file header (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header);
    }
}
