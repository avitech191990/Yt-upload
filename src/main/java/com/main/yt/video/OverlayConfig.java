package com.main.yt.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "video")
public class OverlayConfig {

    public String inputDir;
    public String outputDir;
    public String logoPath;
    public int logoWidth;

    public double bgVolume;

    public List<String> bgAudios;
    public List<String> quotes;
    public List<String> fonts;
    public List<TextPosition> positions;

    public int fontSize;
    public int safeTextWidth;


    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public int getLogoWidth() {
        return logoWidth;
    }

    public void setLogoWidth(int logoWidth) {
        this.logoWidth = logoWidth;
    }

    public double getBgVolume() {
        return bgVolume;
    }

    public void setBgVolume(double bgVolume) {
        this.bgVolume = bgVolume;
    }

    public List<String> getBgAudios() {
        return bgAudios;
    }

    public void setBgAudios(List<String> bgAudios) {
        this.bgAudios = bgAudios;
    }

    public List<String> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<String> quotes) {
        this.quotes = quotes;
    }

    public List<String> getFonts() {
        return fonts;
    }

    public void setFonts(List<String> fonts) {
        this.fonts = fonts;
    }

    public List<TextPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<TextPosition> positions) {
        this.positions = positions;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getSafeTextWidth() {
        return safeTextWidth;
    }

    public void setSafeTextWidth(int safeTextWidth) {
        this.safeTextWidth = safeTextWidth;
    }
}
