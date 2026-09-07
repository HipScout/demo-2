package com.example.gitlabscanner.scanner;

import com.example.gitlabscanner.client.GitLabApiClient;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.SeverityLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ExposedSecretsScanner {
    private static final List<SecretPattern> SECRET_PATTERNS = List.of(
        new SecretPattern("AWS Access Key", "AKIA[0-9A-Z]{16}", SeverityLevel.HIGH),
        new SecretPattern("AWS Secret Key", "aws_secret_access_key\\s*=\\s*[A-Za-z0-9/+=]{40}", SeverityLevel.HIGH),
        new SecretPattern("GitHub Personal Token", "ghp_[0-9a-zA-Z]{36}", SeverityLevel.HIGH),
        new SecretPattern("GitHub OAuth Token", "gho_[0-9a-zA-Z]{36}", SeverityLevel.HIGH),
        new SecretPattern("GitHub App Token", "ghu_[0-9a-zA-Z]{36}", SeverityLevel.HIGH),
        new SecretPattern("GitLab Personal Token", "glpat-[0-9a-zA-Z_-]{20,}", SeverityLevel.HIGH),
        new SecretPattern("Private Key", "-----BEGIN\\s*(RSA|DSA|EC|OPENSSH|PGP)\\s*PRIVATE KEY", SeverityLevel.HIGH),
        new SecretPattern("API Key Generic", "api[_-]?key[\\s]*[=:][\\s]*['\\\"]?[A-Za-z0-9_-]{20,}['\\\"]?", SeverityLevel.MEDIUM),
        new SecretPattern("Password Assignment", "password[\\s]*[=:][\\s]*['\\\"]?[A-Za-z0-9!@#$%^&*_-]{8,}['\\\"]?", SeverityLevel.MEDIUM),
        new SecretPattern("Database Connection String", "mongodb://.*:[^@]+@", SeverityLevel.MEDIUM),
        new SecretPattern("Google API Key", "AIza[0-9A-Za-z\\-_]{35}", SeverityLevel.MEDIUM),
        new SecretPattern("Slack Token", "xox[pbaorb]-[0-9a-zA-Z]{8,}", SeverityLevel.MEDIUM),
        new SecretPattern("JWT Token", "eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+", SeverityLevel.LOW)
    );

    public List<Risk> scan(GitLabApiClient client, Long projectId, String filename) {
        List<Risk> risks = new ArrayList<>();
        
        try {
            String content = client.getFileContent(projectId, filename);
            if (content != null && !content.isEmpty()) {
                for (SecretPattern pattern : SECRET_PATTERNS) {
                    if (Pattern.compile(pattern.regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                            .matcher(content).find()) {
                        risks.add(new Risk(
                            RiskCategory.EXPOSED_SECRETS,
                            "Potential exposed secret detected: " + pattern.name,
                            pattern.severity,
                            filename + " (line with secret not shown for safety)"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore file read errors
        }
        
        return risks;
    }

    private static class SecretPattern {
        String name;
        String regex;
        SeverityLevel severity;

        SecretPattern(String name, String regex, SeverityLevel severity) {
            this.name = name;
            this.regex = regex;
            this.severity = severity;
        }
    }
}
