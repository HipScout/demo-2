package com.example.gitlabscanner;

import com.example.gitlabscanner.client.GitLabApiClient;
import com.example.gitlabscanner.dto.GitLabProjectDTO;
import com.example.gitlabscanner.model.Project;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.scanner.ExposedSecretsScanner;
import com.example.gitlabscanner.scanner.MissingMetadataScanner;
import com.example.gitlabscanner.scanner.SensitiveFilesScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GitLabScannerIntegrationTest {

    @Autowired
    private GitLabApiClient gitLabApiClient;

    @Autowired
    private ScannerService scannerService;

    @Autowired
    private SensitiveFilesScanner sensitiveFilesScanner;

    @Autowired
    private ExposedSecretsScanner exposedSecretsScanner;

    @Autowired
    private MissingMetadataScanner missingMetadataScanner;

    @Test
    public void testGitLabApiClientFetchesGroupProjects() {
        // Test with a known public GitLab group
        List<GitLabProjectDTO> projects = gitLabApiClient.getGroupPublicProjects("gitlab-examples");
        
        assertNotNull(projects, "Projects list should not be null");
        org.junit.jupiter.api.Assumptions.assumeTrue(!projects.isEmpty(), "GitLab API is unreachable or returned 0 projects, skipping live test");
        
        // Verify project structure
        GitLabProjectDTO firstProject = projects.get(0);
        assertNotNull(firstProject.getId(), "Project ID should not be null");
        assertNotNull(firstProject.getName(), "Project name should not be null");
        assertNotNull(firstProject.getWebUrl(), "Project web URL should not be null");
    }

    @Test
    public void testScanGroupProjects() {
        // Test scanning a public group's projects
        ScanResult result = scannerService.scanGroupProjects("gitlab-examples", null);
        
        assertNotNull(result, "Scan result should not be null");
        assertNotNull(result.getGroupName(), "Group name should be set");
        assertEquals("gitlab-examples", result.getGroupName());
        assertNotNull(result.getScanTime(), "Scan time should be set");
    }

    @Test
    public void testScanResultStructure() {
        ScanResult result = scannerService.scanGroupProjects("gitlab-examples", null);
        
        // Check result structure
        assertNotNull(result.getProjectsScanned(), "Projects scanned should not be null");
        org.junit.jupiter.api.Assumptions.assumeTrue(!result.getProjectsScanned().isEmpty(), "No projects scanned, skipping live structure checks");
        
        // Check project structure
        Project firstProject = result.getProjectsScanned().get(0);
        assertNotNull(firstProject.getId(), "Project ID should be set");
        assertNotNull(firstProject.getName(), "Project name should be set");
        assertNotNull(firstProject.getRisks(), "Project risks should be initialized");
    }

    @Test
    public void testScanStatistics() {
        ScanResult result = scannerService.scanGroupProjects("gitlab-examples", null);
        
        // Check statistics
        assertEquals(result.getProjectsScanned().size(), result.getTotalProjects(),
            "Total projects should match scanned projects count");
        
        // Calculate projects with risks
        long projectsWithRisks = result.getProjectsScanned().stream()
            .filter(p -> !p.getRisks().isEmpty())
            .count();
        assertEquals(projectsWithRisks, result.getProjectsWithRisks(),
            "Projects with risks count should be accurate");
        
        // Verify highest severity is correctly calculated
        if (result.getProjectsWithRisks() > 0) {
            assertNotNull(result.getHighestSeverity(),
                "Highest severity should be set if there are risks");
        }
    }

    @Test
    public void testNonExistentGroupHandling() {
        // Test with a non-existent group
        ScanResult result = scannerService.scanGroupProjects("nonexistent_group_" + System.currentTimeMillis(), null);
        
        assertNotNull(result, "Scan result should not be null");
        assertTrue(result.getProjectsScanned().isEmpty(),
            "Should return empty projects list for non-existent group");
        assertEquals(0, result.getTotalProjects());
    }
}

