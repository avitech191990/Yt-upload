package com.main.yt.video;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

@Service
public class VideoOverlayService {

    private final OverlayConfig config;

    public VideoOverlayService(OverlayConfig config) {
        this.config = config;
    }

    private final Random random = new Random();
    private static final Path QUOTE_INDEX = Path.of(".quote-index");
    private final String ffmpegCmd = "ffmpeg";

    private final List<TextPosition> positions =
            List.of(TextPosition.MIDDLE, TextPosition.MIDDLE_TOP, TextPosition.MIDDLE_BOTTOM);

    public File process(
            File video,
            String quote,
            File audio
    ) throws Exception {

        //String fontName = fonts.get(random.nextInt(fonts.size()));
        TextPosition pos = positions.get(random.nextInt(positions.size()));
        String fontName = config.getFonts()
                .get(random.nextInt(config.getFonts().size()));

        File textPng = createTextImage(quote, fontName);
        System.out.println("Text PNG size: " + textPng.length());
        File logoPng = loadLogoFromResources(config.logoPath);

        //local run
        //File output = new File(config.outputDir, video.getName());

        File outputDir = new File("output");
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            System.out.println("📁 Output directory created: " + created);
        }

        File output = new File(outputDir, video.getName());


        String overlayY = switch (pos) {
            case MIDDLE -> "(H-h)/2";
            case MIDDLE_TOP -> "(H-h)/2-220";
            case MIDDLE_BOTTOM -> "(H-h)/2+220";
        };

        File filterFile = File.createTempFile("filter-", ".txt");
        filterFile.deleteOnExit();

        String filterScript =
                "[1:v]format=rgba[text];\n" +
                "[2:v]scale=" + config.getLogoWidth() + ":-1[logo];\n" +
                "[0:v][text]overlay=(W-w)/2:" + overlayY + "[v1];\n" +
                "[v1][logo]overlay=20:H-h-170[vout];\n" +
                "[3:a]volume=1.0[aout];\n";

        Files.writeString(filterFile.toPath(), filterScript);

        System.out.println("===== FILTER SCRIPT =====");
        System.out.println(filterScript);
        System.out.println("=========================");


        ProcessBuilder pb = new ProcessBuilder(
                ffmpegCmd,
                "-y",

                "-i", video.getAbsolutePath(),   // 0 video
                "-i", textPng.getAbsolutePath(), // 1 text png
                "-i", logoPng.getAbsolutePath(), // 2 logo
                "-stream_loop", "-1",
                "-i", audio.getAbsolutePath(),   // 3 bg audio

                "-an", // 🔥 REMOVE original video audio

                "-filter_complex_script", filterFile.getAbsolutePath(),

                "-map", "[vout]",
                "-map", "[aout]",

                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-shortest",

                output.getAbsolutePath()
        );

        // 🔥 THIS IS CRITICAL
        pb.redirectErrorStream(true);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();

        // 🔥 READ ALL OUTPUT
        //textPng.delete();

        if (exit != 0) {
            System.err.println("❌ FFmpeg failed. Command:");
            System.err.println(String.join(" ", pb.command()));
            throw new RuntimeException("FFmpeg failed for " + video.getName());
        }

        System.out.println("🎯 Text position chosen: " + pos);
        System.out.println("🎯 Overlay Y expression: " + overlayY);
        System.out.println("✅ Processed video path  : " + output.getAbsolutePath());
        System.out.println("✅ Exists after FFmpeg   : " + output.exists());
        System.out.println("✅ Size (bytes)          : " + output.length());
        System.out.println("OUTPUT PATH = " + output.getAbsolutePath());
        System.out.println("OUTPUT DIR EXISTS = " + output.getParentFile().exists());


        return output;
    }

    private File createTextImage(String text, String fontName) throws Exception {

        int padding = 40;
        int maxTextWidth = config.safeTextWidth - padding * 2;

        Font font = new Font(fontName, Font.BOLD, config.fontSize);

        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = temp.createGraphics();
        tg.setFont(font);
        FontMetrics fm = tg.getFontMetrics();

        List<String> lines = wrapText(text, fm, maxTextWidth);
        int height = lines.size() * fm.getHeight() + padding * 2;

        tg.dispose();

        BufferedImage img =
                new BufferedImage(config.safeTextWidth, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setFont(font);
        g.setColor(Color.WHITE);

        int y = padding + fm.getAscent();
        for (String line : lines) {
            int x = (config.safeTextWidth - fm.stringWidth(line)) / 2;
            g.drawString(line, x, y);
            y += fm.getHeight();
        }

        g.dispose();

        File png = File.createTempFile("text-", ".png");
        ImageIO.write(img, "png", png);
        return png;
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (fm.stringWidth(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private File loadLogoFromResources(String logoPath) throws Exception {

        if (!logoPath.startsWith("classpath:")) {
            return new File(logoPath); // fallback for absolute paths
        }

        String resourcePath = logoPath.replace("classpath:", "");

        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("Logo not found in resources: " + resourcePath);
        }

        File tempLogo = File.createTempFile("logo-", ".png");
        //tempLogo.deleteOnExit();

        try (is) {
            Files.copy(is, tempLogo.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return tempLogo;
    }

}


