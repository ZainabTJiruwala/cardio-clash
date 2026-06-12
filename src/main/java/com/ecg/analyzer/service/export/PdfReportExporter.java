package com.ecg.analyzer.service.export;

import com.ecg.analyzer.model.Anomaly;
import com.ecg.analyzer.service.AnalysisService;
import com.ecg.analyzer.util.ChartExporter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.scene.chart.LineChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for exporting ECG analysis results to PDF format.
 */
public class PdfReportExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfReportExporter.class);
    
    // PDF styling constants
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADING_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
    
    /**
     * Exports analysis results to a PDF file.
     * 
     * @param result The analysis result to export
     * @param chart The ECG chart to include in the report
     * @param outputFile The output PDF file
     * @param sourceFileName The original ECG data filename
     * @throws IOException if export fails
     */
    public void exportReport(AnalysisService.AnalysisResult result, 
                            LineChart<Number, Number> chart,
                            File outputFile,
                            String sourceFileName) throws IOException {
        
        logger.info("Exporting PDF report to: {}", outputFile.getAbsolutePath());
        
        try {
            // Create PDF document
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            
            document.open();
            
            // Add content
            addHeader(document, sourceFileName);
            addMetadataTable(document, result);
            addChartImage(document, chart);
            addMetricsSummary(document, result);
            addAnomalyTable(document, result);
            addFooter(document);
            
            document.close();
            
            logger.info("Successfully exported PDF report");
            
        } catch (DocumentException e) {
            logger.error("Failed to create PDF document", e);
            throw new IOException("Failed to export PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds the report header.
     */
    private void addHeader(Document document, String sourceFileName) throws DocumentException {
        // Title
        Paragraph title = new Paragraph("ECG Analysis Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        
        // Timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Paragraph date = new Paragraph("Generated: " + timestamp, NORMAL_FONT);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(5);
        document.add(date);
        
        // Source file
        Paragraph source = new Paragraph("Source File: " + sourceFileName, NORMAL_FONT);
        source.setAlignment(Element.ALIGN_CENTER);
        source.setSpacingAfter(20);
        document.add(source);
        
        // Separator line
        document.add(new Paragraph(" "));
    }
    
    /**
     * Adds basic metadata table.
     */
    private void addMetadataTable(Document document, AnalysisService.AnalysisResult result) throws DocumentException {
        Paragraph heading = new Paragraph("Signal Information", HEADING_FONT);
        heading.setSpacingBefore(10);
        heading.setSpacingAfter(5);
        document.add(heading);
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        
        addMetadataRow(table, "Record ID", result.signal().recordId());
        addMetadataRow(table, "Sampling Rate", result.signal().samplingRate() + " Hz");
        addMetadataRow(table, "Total Samples", String.valueOf(result.signal().dataPoints().size()));
        addMetadataRow(table, "Duration", String.format("%.2f seconds", result.signal().duration()));
        addMetadataRow(table, "Processing Time", result.processingTimeMs() + " ms");
        
        document.add(table);
    }
    
    /**
     * Adds a row to metadata table.
     */
    private void addMetadataRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    /**
     * Adds the ECG chart image to the PDF.
     */
    private void addChartImage(Document document, LineChart<Number, Number> chart) throws DocumentException, IOException {
        Paragraph heading = new Paragraph("ECG Waveform", HEADING_FONT);
        heading.setSpacingBefore(10);
        heading.setSpacingAfter(5);
        document.add(heading);
        
        try {
            // Capture chart as image
            BufferedImage chartImage = ChartExporter.captureChartImage(chart);
            
            // Convert to iText Image
            Image pdfImage = Image.getInstance(chartImage, null);
            
            // Scale to fit page width
            float maxWidth = document.getPageSize().getWidth() - 80;
            if (pdfImage.getWidth() > maxWidth) {
                float scaleFactor = maxWidth / pdfImage.getWidth();
                pdfImage.scalePercent(scaleFactor * 100);
            }
            
            pdfImage.setAlignment(Element.ALIGN_CENTER);
            pdfImage.setSpacingAfter(15);
            document.add(pdfImage);
            
        } catch (Exception e) {
            logger.warn("Failed to add chart image to PDF", e);
            document.add(new Paragraph("(Chart image unavailable)", SMALL_FONT));
        }
    }
    
    /**
     * Adds metrics summary section.
     */
    private void addMetricsSummary(Document document, AnalysisService.AnalysisResult result) throws DocumentException {
        Paragraph heading = new Paragraph("Metrics Summary", HEADING_FONT);
        heading.setSpacingBefore(10);
        heading.setSpacingAfter(5);
        document.add(heading);
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        
        // Header row
        addTableHeader(table, "Metric");
        addTableHeader(table, "Value");
        addTableHeader(table, "Status");
        addTableHeader(table, "Normal Range");
        
        // Heart Rate
        double hr = result.getAverageHeartRate();
        String hrStatus = hr < 60 ? "Bradycardia" : hr > 100 ? "Tachycardia" : "Normal";
        addTableRow(table, "Heart Rate", String.format("%.1f BPM", hr), hrStatus, "60-100 BPM");
        
        // R-Peaks Count
        addTableRow(table, "R-Peaks Detected", String.valueOf(result.rPeakIndices().size()), "-", "-");
        
        // Anomaly Counts
        addTableRow(table, "High Severity Anomalies", 
            String.valueOf(result.getAnomalyCountBySeverity(Anomaly.Severity.HIGH)), "-", "0");
        addTableRow(table, "Medium Severity Anomalies", 
            String.valueOf(result.getAnomalyCountBySeverity(Anomaly.Severity.MEDIUM)), "-", "0");
        addTableRow(table, "Low Severity Anomalies", 
            String.valueOf(result.getAnomalyCountBySeverity(Anomaly.Severity.LOW)), "-", "0");
        
        document.add(table);
    }
    
    /**
     * Adds detected anomalies table.
     */
    private void addAnomalyTable(Document document, AnalysisService.AnalysisResult result) throws DocumentException {
        Paragraph heading = new Paragraph("Detected Anomalies", HEADING_FONT);
        heading.setSpacingBefore(10);
        heading.setSpacingAfter(5);
        document.add(heading);
        
        if (result.anomalies().isEmpty()) {
            Paragraph noAnomalies = new Paragraph("No anomalies detected.", NORMAL_FONT);
            noAnomalies.setSpacingAfter(15);
            document.add(noAnomalies);
            return;
        }
        
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        
        try {
            table.setWidths(new float[]{1.5f, 2.5f, 1.5f, 1.5f, 4f});
        } catch (DocumentException e) {
            logger.warn("Failed to set column widths", e);
        }
        
        // Header
        addTableHeader(table, "Time (s)");
        addTableHeader(table, "Type");
        addTableHeader(table, "Severity");
        addTableHeader(table, "Confidence");
        addTableHeader(table, "Details");
        
        // Anomaly rows
        for (Anomaly anomaly : result.anomalies()) {
            addTableRow(table, 
                String.format("%.2f", anomaly.timestamp()),
                anomaly.type().getDisplayName(),
                anomaly.severity().getLabel(),
                anomaly.getConfidencePercent() + "%",
                anomaly.details()
            );
        }
        
        document.add(table);
    }
    
    /**
     * Adds table header cell.
     */
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE)));
        cell.setBackgroundColor(new BaseColor(52, 73, 94)); // Dark blue-gray
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
    
    /**
     * Adds table data row.
     */
    private void addTableRow(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, NORMAL_FONT));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
    
    /**
     * Adds footer to the document.
     */
    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
            "This report is for research and educational purposes only. " +
            "Not intended for clinical diagnosis or medical decision-making.",
            SMALL_FONT
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
        
        Paragraph appInfo = new Paragraph("Generated by ECG Anomaly Analyzer v1.0", SMALL_FONT);
        appInfo.setAlignment(Element.ALIGN_CENTER);
        document.add(appInfo);
    }
}
