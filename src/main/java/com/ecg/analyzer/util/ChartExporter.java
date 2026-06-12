package com.ecg.analyzer.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for exporting JavaFX charts as images.
 */
public class ChartExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ChartExporter.class);
    
    /**
     * Exports a LineChart as a PNG image.
     * 
     * @param chart The chart to export
     * @param file The target file
     * @throws IOException if export fails
     */
    public static void exportChartAsPng(LineChart<Number, Number> chart, File file) throws IOException {
        exportChart(chart, file, "png");
    }
    
    /**
     * Exports a LineChart as a JPG image.
     * 
     * @param chart The chart to export
     * @param file The target file
     * @throws IOException if export fails
     */
    public static void exportChartAsJpg(LineChart<Number, Number> chart, File file) throws IOException {
        exportChart(chart, file, "jpg");
    }
    
    /**
     * Internal method to export chart with specified format.
     */
    private static void exportChart(LineChart<Number, Number> chart, File file, String format) throws IOException {
        logger.info("Exporting chart to {} as {}", file.getName(), format.toUpperCase());
        
        try {
            // Take a snapshot of the chart
            WritableImage image = chart.snapshot(new SnapshotParameters(), null);
            
            // Convert to BufferedImage
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            
            // Write to file
            ImageIO.write(bufferedImage, format, file);
            
            logger.info("Successfully exported chart to: {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export chart", e);
            throw new IOException("Failed to export chart: " + e.getMessage(), e);
        }
    }
    
    /**
     * Returns a snapshot of the chart as a BufferedImage.
     * Useful for embedding in PDF reports.
     * 
     * @param chart The chart to capture
     * @return BufferedImage of the chart
     */
    public static BufferedImage captureChartImage(LineChart<Number, Number> chart) {
        logger.debug("Capturing chart snapshot");
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        return SwingFXUtils.fromFXImage(image, null);
    }
}
