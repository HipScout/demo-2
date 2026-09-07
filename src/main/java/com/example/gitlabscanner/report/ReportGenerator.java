package com.example.gitlabscanner.report;

import com.example.gitlabscanner.model.Project;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.model.SeverityLevel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class ReportGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
            new JsonPrimitive(src.format(DATE_FORMATTER)))
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
            LocalDateTime.parse(json.getAsString(), DATE_FORMATTER))
        .setPrettyPrinting()
        .create();

    public String generateCliReport(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n╔════════════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║ GitLab Repository Risk Scan Report %-35s║\n", ""));
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        
        String scanTarget = result.getUsername() != null ? 
            "User: " + result.getUsername() : 
            "Group: " + result.getGroupName();
        sb.append(String.format("║ %-66s ║\n", scanTarget));
        sb.append(String.format("║ Scan Time: %-56s ║\n", result.getFormattedScanTime()));
        sb.append(String.format("║ Total Projects: %d | Projects with Risks: %d %-36s ║\n", 
            result.getTotalProjects(), result.getProjectsWithRisks(), ""));
        
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ Project Name               │ Issues                │ Severity     ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        
        for (Project project : result.getProjectsScanned()) {
            if (!project.getRisks().isEmpty()) {
                String projectName = truncate(project.getName(), 25);
                String issues = truncate(formatIssues(project), 20);
                String severity = project.getMaxSeverity() != null ? 
                    project.getMaxSeverity().toString() : "NONE";
                
                sb.append(String.format("║ %-25s │ %-20s │ %-13s ║\n", 
                    projectName, issues, severity));
            }
        }
        
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ HIGH: %d | MEDIUM: %d | LOW: %d %-37s ║\n",
            result.getHighRiskCount(),
            result.getMediumRiskCount(),
            result.getLowRiskCount(),
            ""));
        sb.append("╚════════════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }

    public String generateDetailedCliReport(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateCliReport(result));
        sb.append("\n\n═══════════════════════════════════════════════════════════════════\n");
        sb.append("DETAILED FINDINGS\n");
        sb.append("═══════════════════════════════════════════════════════════════════\n\n");
        
        for (Project project : result.getProjectsScanned()) {
            if (!project.getRisks().isEmpty()) {
                sb.append(String.format("📦 Project: %s\n", project.getName()));
                sb.append(String.format("   URL: %s\n\n", project.getWebUrl()));
                
                for (Risk risk : project.getRisks()) {
                    String severity = String.format("[%s]", risk.getSeverity());
                    sb.append(String.format("   %s %s\n", severity, risk.getCategory().getDisplayName()));
                    sb.append(String.format("       └─ %s\n", risk.getDescription()));
                    sb.append(String.format("       └─ Evidence: %s\n\n", risk.getEvidence()));
                }
            }
        }
        
        return sb.toString();
    }

    public String generateJsonReport(ScanResult result) {
        return gson.toJson(result);
    }

    public byte[] generatePdfReport(ScanResult result) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(41, 128, 185));
            Font subTitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
            Font regularFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

            // Title
            Paragraph title = new Paragraph("GitLab Repository Risk Scan Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            // Target and Time
            String target = result.getUsername() != null ? "User: " + result.getUsername() : "Group: " + result.getGroupName();
            Paragraph info = new Paragraph(target + " | Scan Time: " + result.getFormattedScanTime(), subTitleFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(15);
            document.add(info);

            // Summary Statistics Table
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(15);

            addCell(summaryTable, "Total Projects", boldFont, BaseColor.LIGHT_GRAY);
            addCell(summaryTable, "Projects with Risks", boldFont, BaseColor.LIGHT_GRAY);
            addCell(summaryTable, "Highest Severity", boldFont, BaseColor.LIGHT_GRAY);
            addCell(summaryTable, "High / Med / Low", boldFont, BaseColor.LIGHT_GRAY);

            addCell(summaryTable, String.valueOf(result.getTotalProjects()), regularFont, null);
            addCell(summaryTable, String.valueOf(result.getProjectsWithRisks()), regularFont, null);
            addCell(summaryTable, result.getHighestSeverity() != null ? result.getHighestSeverity().toString() : "NONE", regularFont, null);
            addCell(summaryTable, result.getHighRiskCount() + " / " + result.getMediumRiskCount() + " / " + result.getLowRiskCount(), regularFont, null);

            document.add(summaryTable);

            // Findings Table
            Paragraph findingsHeader = new Paragraph("Findings Summary", new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD));
            findingsHeader.setSpacingAfter(8);
            document.add(findingsHeader);

            PdfPTable table = new PdfPTable(new float[]{2.5f, 2.2f, 4.3f, 1.5f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);

            BaseColor headerBg = new BaseColor(52, 73, 94);
            addCell(table, "Project Name", headerFont, headerBg);
            addCell(table, "Risk Category", headerFont, headerBg);
            addCell(table, "Description", headerFont, headerBg);
            addCell(table, "Severity", headerFont, headerBg);

            for (Project project : result.getProjectsScanned()) {
                if (project.getRisks().isEmpty()) {
                    addCell(table, project.getName(), regularFont, null);
                    addCell(table, "-", regularFont, null);
                    addCell(table, "No risks detected", regularFont, null);
                    addCell(table, "CLEAN", regularFont, new BaseColor(212, 239, 223));
                } else {
                    for (Risk risk : project.getRisks()) {
                        addCell(table, project.getName(), regularFont, null);
                        addCell(table, risk.getCategory().getDisplayName(), regularFont, null);
                        addCell(table, risk.getDescription() + " (" + risk.getEvidence() + ")", regularFont, null);

                        BaseColor sevBg = risk.getSeverity() == SeverityLevel.HIGH ? new BaseColor(242, 215, 213) :
                                         risk.getSeverity() == SeverityLevel.MEDIUM ? new BaseColor(252, 243, 207) :
                                         new BaseColor(254, 249, 231);
                        addCell(table, risk.getSeverity().toString(), boldFont, sevBg);
                    }
                }
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        table.addCell(cell);
    }

    private String formatIssues(Project project) {
        return project.getRisks().stream()
            .map(r -> r.getCategory().getDisplayName())
            .distinct()
            .limit(2)
            .collect(Collectors.joining(", "));
    }

    private String truncate(String str, int length) {
        if (str == null) return "";
        if (str.length() > length) {
            return str.substring(0, length - 3) + "...";
        }
        return String.format("%-" + length + "s", str);
    }
}
