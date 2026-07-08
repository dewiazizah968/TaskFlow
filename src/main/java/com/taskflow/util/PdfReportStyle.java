package com.taskflow.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.core.io.ClassPathResource;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

public final class PdfReportStyle {

    private PdfReportStyle() {}

    // ── Brand colors (matches TaskFlow's web UI) ──
    public static final Color PRIMARY       = new Color(0x63, 0x66, 0xf1); // #6366f1
    public static final Color PRIMARY_DARK  = new Color(0x4f, 0x46, 0xe5); // #4f46e5
    public static final Color ACCENT_PURPLE = new Color(0xa8, 0x55, 0xf7); // #a855f7
    public static final Color ACCENT_GREEN  = new Color(0x22, 0xc5, 0x5e); // #22c55e
    public static final Color ACCENT_AMBER  = new Color(0xf5, 0x9e, 0x0b); // #f59e0b
    public static final Color ACCENT_RED    = new Color(0xef, 0x44, 0x44); // #ef4444
    public static final Color ACCENT_CYAN   = new Color(0x06, 0xb6, 0xd4); // #06b6d4
    public static final Color TEXT_DARK     = new Color(0x1f, 0x29, 0x37); // #1f2937
    public static final Color TEXT_MUTED    = new Color(0x6b, 0x72, 0x80); // #6b7280
    public static final Color BORDER_GRAY   = new Color(0xe5, 0xe7, 0xeb); // #e5e7eb
    public static final Color ROW_STRIPE    = new Color(0xf9, 0xfa, 0xfb); // #f9fafb
    public static final Color WHITE         = Color.WHITE;

    // Symbols safe to use with the embedded DejaVu Sans font
    public static final String ICON_CHECK   = "\u2713"; // ✓
    public static final String ICON_CROSS   = "\u2717"; // ✗
    public static final String ICON_STAR    = "\u2605"; // ★
    public static final String ICON_WARNING = "\u26A0"; // ⚠
    public static final String ICON_DOT     = "\u25CF"; // ●
    public static final String ICON_ARROW   = "\u2192"; // →

    private static BaseFont dejaVuRegular;
    private static BaseFont dejaVuBold;

    private static synchronized BaseFont regularBase() {
        if (dejaVuRegular == null) {
            dejaVuRegular = loadBaseFont("fonts/DejaVuSans.ttf");
        }
        return dejaVuRegular;
    }

    private static synchronized BaseFont boldBase() {
        if (dejaVuBold == null) {
            dejaVuBold = loadBaseFont("fonts/DejaVuSans-Bold.ttf");
        }
        return dejaVuBold;
    }

    private static BaseFont loadBaseFont(String classpathTtf) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathTtf);
            byte[] fontBytes = resource.getInputStream().readAllBytes();
            return BaseFont.createFont(classpathTtf, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, fontBytes, null);
        } catch (Exception e) {
            // Fall back to a standard PDF font so report generation never hard-fails
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public static Font title(float size) {
        return new Font(regularBase(), size, Font.BOLD, WHITE);
    }

    public static Font h1(float size, Color color) {
        return new Font(boldBase(), size, Font.NORMAL, color);
    }

    public static Font body(float size, Color color) {
        return new Font(regularBase(), size, Font.NORMAL, color);
    }

    public static Font bold(float size, Color color) {
        return new Font(boldBase(), size, Font.NORMAL, color);
    }

    /**
     * Top banner with the TaskFlow logo (if present on classpath), report title and
     * subtitle, on a brand-colored background.
     */
    public static PdfPTable buildBanner(String titleText, String subtitle) {
        PdfPTable banner = new PdfPTable(new float[]{1f, 4f});
        banner.setWidthPercentage(100);
        banner.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setBackgroundColor(PRIMARY_DARK);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(10f);

        Image logo = loadLogo();
        if (logo != null) {
            // Logo asset is a 500x500 square PNG; fit inside a 55x55pt box so it sits
            // neatly next to the title text without dominating the banner.
            logo.scaleToFit(55f, 55f);
            logoCell.addElement(logo);
        } else {
            Paragraph fallback = new Paragraph("TF", h1(22f, WHITE));
            fallback.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(fallback);
        }
        banner.addCell(logoCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setBackgroundColor(PRIMARY_DARK);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setPaddingLeft(16f);
        textCell.setPaddingTop(14f);
        textCell.setPaddingBottom(14f);

        Paragraph t = new Paragraph(titleText, title(20f));
        t.setSpacingAfter(4f);
        textCell.addElement(t);

        Paragraph s = new Paragraph(subtitle, body(10.5f, new Color(0xe0, 0xe7, 0xff)));
        textCell.addElement(s);

        banner.addCell(textCell);

        return banner;
    }

    /** Loads the TaskFlow logo from static/images/logo.png, if present. Returns null otherwise. */
    public static Image loadLogo() {
        try {
            ClassPathResource resource = new ClassPathResource("static/images/logo.png");
            if (!resource.exists()) return null;
            try (InputStream is = resource.getInputStream()) {
                return Image.getInstance(is.readAllBytes());
            }
        } catch (IOException | BadElementException e) {
            return null;
        }
    }

    /** A colored summary "stat card" cell for KPI rows (Total Tasks, Progress, etc). */
    public static PdfPCell statCard(String label, String value, Color accent) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, h1(18f, accent)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setPaddingBottom(2f);
        inner.addCell(valueCell);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, body(9f, TEXT_MUTED)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        inner.addCell(labelCell);

        PdfPCell wrapper = new PdfPCell(inner);
        wrapper.setBackgroundColor(ROW_STRIPE);
        wrapper.setBorderColor(BORDER_GRAY);
        wrapper.setBorderWidth(1f);
        wrapper.setPadding(12f);
        return wrapper;
    }

    /** Small colored pill-like badge cell, e.g. for status or priority values inside a table. */
    public static PdfPCell badgeCell(String text, Color bg, Color fg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold(8.5f, fg)));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        return cell;
    }

    /**
     * Wraps a badge (status/priority pill) inside a bordered outer cell so it lines up
     * cleanly with the rest of a zebra-striped table row.
     */
    public static PdfPCell wrapBadge(String text, Color accent, Color rowBg) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        inner.addCell(badgeCell(text, tint(accent), accent));

        PdfPCell outer = new PdfPCell(inner);
        outer.setBackgroundColor(rowBg);
        outer.setBorderColor(BORDER_GRAY);
        outer.setPadding(4f);
        outer.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return outer;
    }

    /** Light tinted background for a badge, derived from the accent color. */
    public static Color tint(Color accent) {
        int r = accent.getRed(), g = accent.getGreen(), b = accent.getBlue();
        return new Color(
                Math.min(255, r + (255 - r) * 85 / 100),
                Math.min(255, g + (255 - g) * 85 / 100),
                Math.min(255, b + (255 - b) * 85 / 100)
        );
    }

    /** A plain body-text table cell with consistent padding/border for use in data tables. */
    public static PdfPCell tableCell(String text, Color rowBg, Color textColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, body(9.5f, textColor)));
        cell.setBackgroundColor(rowBg);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(6f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public static Color statusColor(String status) {
        if (status == null) return TEXT_MUTED;
        return switch (status) {
            case "DONE" -> ACCENT_GREEN;
            case "IN_PROGRESS" -> ACCENT_CYAN;
            default -> TEXT_MUTED; // TODO
        };
    }

    public static Color priorityColor(String priority) {
        if (priority == null) return TEXT_MUTED;
        return switch (priority) {
            case "HIGH" -> ACCENT_RED;
            case "MEDIUM" -> ACCENT_AMBER;
            default -> ACCENT_GREEN; // LOW
        };
    }

    public static String statusIcon(String status) {
        if ("DONE".equals(status)) return ICON_CHECK + " ";
        if ("IN_PROGRESS".equals(status)) return ICON_ARROW + " ";
        return ICON_DOT + " ";
    }

    public static String priorityIcon(String priority) {
        if ("HIGH".equals(priority)) return ICON_WARNING + " ";
        if ("MEDIUM".equals(priority)) return ICON_STAR + " ";
        return "";
    }

    /** A section heading with a small colored accent bar on the left. */
    public static PdfPTable sectionHeading(String text) {
        PdfPTable table = new PdfPTable(new float[]{0.06f, 4f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(16f);
        table.setSpacingAfter(8f);

        PdfPCell bar = new PdfPCell(new Phrase(""));
        bar.setBackgroundColor(PRIMARY);
        bar.setBorder(Rectangle.NO_BORDER);
        bar.setFixedHeight(16f);
        table.addCell(bar);

        PdfPCell label = new PdfPCell(new Phrase(text, h1(13f, TEXT_DARK)));
        label.setBorder(Rectangle.NO_BORDER);
        label.setPaddingLeft(8f);
        label.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(label);

        return table;
    }

    /** A ready-to-fill table with a brand-colored header row. */
    public static PdfPTable emptyTableHeader(String[] headers, float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, bold(9.5f, WHITE)));
            cell.setBackgroundColor(PRIMARY);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(7f);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
        return table;
    }

    /** A simple horizontal progress bar built from two colored table cells. */
    public static PdfPTable progressBar(double progressPct) {
        float filled = (float) Math.max(0, Math.min(100, progressPct));
        float empty = 100f - filled;

        PdfPTable bar = new PdfPTable(empty <= 0.001f ? new float[]{1f} : new float[]{filled, empty});
        bar.setWidthPercentage(100);

        PdfPCell filledCell = new PdfPCell(new Phrase(""));
        filledCell.setBackgroundColor(ACCENT_GREEN);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setFixedHeight(10f);
        bar.addCell(filledCell);

        if (empty > 0.001f) {
            PdfPCell emptyCell = new PdfPCell(new Phrase(""));
            emptyCell.setBackgroundColor(BORDER_GRAY);
            emptyCell.setBorder(Rectangle.NO_BORDER);
            emptyCell.setFixedHeight(10f);
            bar.addCell(emptyCell);
        }
        return bar;
    }

    /** A label/value row for simple "info" tables (no borders, just spaced rows). */
    public static void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, bold(9.5f, TEXT_MUTED)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, body(9.5f, TEXT_DARK)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6f);
        table.addCell(valueCell);
    }

    /** Turns any free-form name into a safe filename fragment (letters, numbers, dash). */
    public static String toFileSlug(String raw) {
        if (raw == null || raw.isBlank()) return "untitled";
        String slug = raw.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "untitled" : slug;
    }
}
