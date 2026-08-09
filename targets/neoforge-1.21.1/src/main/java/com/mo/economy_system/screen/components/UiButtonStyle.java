package com.mo.economy_system.screen.components;

public final class UiButtonStyle {
    private int accentColor;
    private int textColor = 0xFFFFFFFF;
    private int bgAlpha = 0x99;
    private int bgAlphaHover = 0xAA;
    private int stripeAlpha = 0xCC;
    private int stripeAlphaHover = 0xFF;
    private int stripeWidth = 4;
    private int glowHeight = 6;
    private int glowAlphaStart = 36;
    private int borderAlpha = 0x20;
    private int borderAlphaHover = 0x40;
    private int padding = 10;
    private boolean textShadow = true;

    public UiButtonStyle(int accentColor) {
        this.accentColor = accentColor;
    }

    public static UiButtonStyle accent(int accentColor) {
        return new UiButtonStyle(accentColor);
    }

    public int getAccentColor() {
        return accentColor;
    }

    public UiButtonStyle setAccentColor(int accentColor) {
        this.accentColor = accentColor;
        return this;
    }

    public int getTextColor() {
        return textColor;
    }

    public UiButtonStyle setTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    public int getBgAlpha() {
        return bgAlpha;
    }

    public UiButtonStyle setBgAlpha(int bgAlpha) {
        this.bgAlpha = bgAlpha;
        return this;
    }

    public int getBgAlphaHover() {
        return bgAlphaHover;
    }

    public UiButtonStyle setBgAlphaHover(int bgAlphaHover) {
        this.bgAlphaHover = bgAlphaHover;
        return this;
    }

    public int getStripeAlpha() {
        return stripeAlpha;
    }

    public UiButtonStyle setStripeAlpha(int stripeAlpha) {
        this.stripeAlpha = stripeAlpha;
        return this;
    }

    public int getStripeAlphaHover() {
        return stripeAlphaHover;
    }

    public UiButtonStyle setStripeAlphaHover(int stripeAlphaHover) {
        this.stripeAlphaHover = stripeAlphaHover;
        return this;
    }

    public int getStripeWidth() {
        return stripeWidth;
    }

    public UiButtonStyle setStripeWidth(int stripeWidth) {
        this.stripeWidth = stripeWidth;
        return this;
    }

    public int getGlowHeight() {
        return glowHeight;
    }

    public UiButtonStyle setGlowHeight(int glowHeight) {
        this.glowHeight = glowHeight;
        return this;
    }

    public int getGlowAlphaStart() {
        return glowAlphaStart;
    }

    public UiButtonStyle setGlowAlphaStart(int glowAlphaStart) {
        this.glowAlphaStart = glowAlphaStart;
        return this;
    }

    public int getBorderAlpha() {
        return borderAlpha;
    }

    public UiButtonStyle setBorderAlpha(int borderAlpha) {
        this.borderAlpha = borderAlpha;
        return this;
    }

    public int getBorderAlphaHover() {
        return borderAlphaHover;
    }

    public UiButtonStyle setBorderAlphaHover(int borderAlphaHover) {
        this.borderAlphaHover = borderAlphaHover;
        return this;
    }

    public int getPadding() {
        return padding;
    }

    public UiButtonStyle setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public boolean isTextShadow() {
        return textShadow;
    }

    public UiButtonStyle setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }
}
