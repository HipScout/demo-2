package com.example.gitlabscanner;

import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.SeverityLevel;
import com.example.gitlabscanner.scanner.MissingMetadataScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MissingMetadataScannerTest {

    private MissingMetadataScanner scanner;

    @BeforeEach
    public void setUp() {
        scanner = new MissingMetadataScanner();
    }

    private GitLabTreeItemDTO createBlob(String name) {
        GitLabTreeItemDTO item = new GitLabTreeItemDTO();
        item.setName(name);
        item.setType("blob");
        item.setPath(name);
        return item;
    }

    @Test
    public void testBothReadmeAndLicenseMissing() {
        List<GitLabTreeItemDTO> tree = List.of(createBlob("index.js"));
        List<Risk> risks = scanner.scan(tree);

        assertEquals(2, risks.size());
        assertTrue(risks.stream().allMatch(r -> r.getCategory() == RiskCategory.MISSING_METADATA));
        assertTrue(risks.stream().allMatch(r -> r.getSeverity() == SeverityLevel.LOW));
        assertTrue(risks.stream().anyMatch(r -> r.getDescription().contains("README")));
        assertTrue(risks.stream().anyMatch(r -> r.getDescription().contains("LICENSE")));
    }

    @Test
    public void testReadmePresentLicenseMissing() {
        List<GitLabTreeItemDTO> tree = List.of(createBlob("README.md"), createBlob("main.py"));
        List<Risk> risks = scanner.scan(tree);

        assertEquals(1, risks.size());
        assertTrue(risks.get(0).getDescription().contains("LICENSE"));
    }

    @Test
    public void testBothPresentProducesNoRisks() {
        List<GitLabTreeItemDTO> tree = List.of(
            createBlob("README.md"),
            createBlob("LICENSE"),
            createBlob("pom.xml")
        );
        List<Risk> risks = scanner.scan(tree);

        assertTrue(risks.isEmpty());
    }
}
