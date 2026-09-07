package com.example.gitlabscanner;

import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.SeverityLevel;
import com.example.gitlabscanner.scanner.ExposedSecretsScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExposedSecretsScannerTest {

    private ExposedSecretsScanner scanner;

    @BeforeEach
    public void setUp() {
        scanner = new ExposedSecretsScanner();
    }

    @Test
    public void testDetectsAwsAccessKey() {
        String content = "const awsKey = 'AKIAIOSFODNN7EXAMPLE';\nconsole.log(awsKey);";
        List<Risk> risks = scanner.scanContent(content, "app.js");

        assertFalse(risks.isEmpty());
        assertTrue(risks.stream().anyMatch(r -> r.getDescription().contains("AWS Access Key")));
        assertTrue(risks.stream().anyMatch(r -> r.getSeverity() == SeverityLevel.HIGH));
    }

    @Test
    public void testDetectsGitLabToken() {
        String content = "gitlab_token: glpat-abcdef1234567890ABCD_xyz";
        List<Risk> risks = scanner.scanContent(content, "config.yml");

        assertFalse(risks.isEmpty());
        assertTrue(risks.stream().anyMatch(r -> r.getDescription().contains("GitLab Personal Token")));
        assertEquals(RiskCategory.EXPOSED_SECRETS, risks.get(0).getCategory());
    }

    @Test
    public void testDetectsPrivateKeyBlock() {
        String content = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0\n-----END RSA PRIVATE KEY-----";
        List<Risk> risks = scanner.scanContent(content, "id_rsa");

        assertFalse(risks.isEmpty());
        assertTrue(risks.stream().anyMatch(r -> r.getDescription().contains("Private Key")));
        assertEquals(SeverityLevel.HIGH, risks.get(0).getSeverity());
    }

    @Test
    public void testCleanFileProducesNoRisks() {
        String content = "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello World\"); } }";
        List<Risk> risks = scanner.scanContent(content, "HelloWorld.java");

        assertTrue(risks.isEmpty());
    }
}
