package com.smartclassroom.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** One place for colours, fonts and the Look and Feel, so every screen looks the same. */
public final class Theme {
    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color PRIMARY_DARK = new Color(0x1D4ED8);
    public static final Color SIDEBAR = new Color(0x0F172A);
    public static final Color SIDEBAR_HOVER = new Color(0x1E293B);
    public static final Color SIDEBAR_TEXT = new Color(0xCBD5E1);
    public static final Color BACKGROUND = new Color(0xF1F5F9);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E8F0);
    public static final Color TEXT = new Color(0x0F172A);
    public static final Color TEXT_MUTED = new Color(0x64748B);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color DANGER_BG = new Color(0xFEE2E2);
    public static final Color ROW_ALT = new Color(0xF8FAFC);
    public static final Color ROW_SELECTED = new Color(0xDBEAFE);
    public static final Color CELL_FILLED = new Color(0xEFF6FF);

    private static final String FAMILY = resolveFamily();
    public static final Font FONT_BASE = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font(FAMILY, Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font FONT_TITLE = new Font(FAMILY, Font.BOLD, 24);
    public static final Font FONT_HEADING = new Font(FAMILY, Font.BOLD, 18);
    public static final Font FONT_BIG_NUMBER = new Font(FAMILY, Font.BOLD, 30);

    private Theme() {
    }

    /** First nice font that exists on this computer, otherwise Java's generic sans-serif. */
    private static String resolveFamily() {
        Set<String> installed = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()).collect(Collectors.toSet());
        for (String candidate : new String[]{"Segoe UI", "Inter", "Roboto", "SF Pro Text", "Helvetica Neue", "Arial"}) {
            if (installed.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    /** Call once, before any window is created. */
    public static void install() {
        try {
            FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#2563EB"));
            FlatLightLaf.setup();
        } catch (RuntimeException | LinkageError e) {
            System.err.println("FlatLaf not available, using the default look: " + e);
        }
        UIManager.put("defaultFont", FONT_BASE);
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
        UIManager.put("OptionPane.messageFont", FONT_BASE);
        UIManager.put("OptionPane.buttonFont", FONT_BASE);
    }
}
