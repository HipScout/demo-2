package com.example.gitlabscanner;

import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.SeverityLevel;
import com.example.gitlabscanner.scanner.SensitiveFilesScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SensitiveFilesScannerTest {

    private SensitiveFilesScanner scanner;

    @BeforeEach
    public void setUp() {
        scanner = new SensitiveFilesScanner();
    }

    private GitLabTreeItemDTO createItem(String name, String path) {
        GitLabTreeItemDTO item = new GitLabTreeItemDTO();
        item.setName(name);
        item.setPath(path);
        item.setType("blob");
        return item;
    }

    @Test
    public void testDetectsDotEnvFile() {
        List<GitLabTreeItemDTO> items = List.of(
            createItem(".env", ".env"),
            createItem("main.go", "main.go")
        );

        List<Risk> risks = scanner.scan(items);
        assertEquals(1, risks.size());
        assertEquals(RiskCategory.SENSITIVE_FILES, risks.get(0).getCategory());
        assertEquals(SeverityLevel.HIGH, risks.get(0).getSeverity());
        assertTrue(risks.get(0).getDescription().contains(".env"));
    }

    @Test
    public void testDetectsPemAndKeyFiles() {
        List<GitLabTreeItemDTO> items = List.of(
            createItem("server.pem", "certs/server.pem"),
            createItem("id_rsa", ".ssh/id_rsa"),
            createItem("secrets.yml", "config/secrets.yml")
        );

        List<Risk> risks = scanner.scan(items);
        assertEquals(3, risks.size());
        for (Risk risk : risks) {
            assertEquals(RiskCategory.SENSITIVE_FILES, risk.getCategory());
            assertEquals(SeverityLevel.HIGH, risk.getSeverity());
        }
    }

    @Test
    public void testDetectsAwsCredentialsPath() {
        List<GitLabTreeItemDTO> items = List.of(
            createItem("credentials", ".aws/credentials")
        );

        List<Risk> risks = scanner.scan(items);
        assertEquals(1, risks.size());
        assertEquals(SeverityLevel.HIGH, risks.get(0).getSeverity());
    }

    @Test
    public void testIgnoresSafeFiles() {
        List<GitLabTreeItemDTO> items = List.of(
            createItem("Application.java", "src/Application.java"),
            createItem("README.md", "README.md"),
            createItem("pom.xml", "pom.xml"),
            createItem("index.html", "index.html")
        );

        List<Risk> risks = scanner.scan(items);
        assertTrue(risks.isEmpty(), "Normal files should not be flagged as sensitive");
    }
}
