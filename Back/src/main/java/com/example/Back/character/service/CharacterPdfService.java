package com.example.Back.character.service;

import com.example.Back.character.entity.Character;
import com.example.Back.template.entity.Template;
import com.example.Back.template.entity.TemplateNode;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * PDF rendering service for character sheets. Uses OpenPDF with the embedded DejaVu Sans font to support Cyrillic glyphs.
 */


@Service
@Slf4j
public class CharacterPdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("ru", "RU"));

    private static final List<String> FONT_CANDIDATES = List.of(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/arialbd.ttf"
    );


    private final Font regular;
    private final Font bold;
    private final Font small;

    public CharacterPdfService() {
        BaseFont base = loadBundledFont("/fonts/DejaVuSans.ttf");
        if (base == null) base = loadUnicodeBaseFont();
        BaseFont baseBold = loadBundledFont("/fonts/DejaVuSans-Bold.ttf");
        if (baseBold == null) baseBold = loadUnicodeBaseFontBold(base);
        this.regular = new Font(base, 11, Font.NORMAL);
        this.bold    = new Font(baseBold, 12, Font.BOLD);
        this.small   = new Font(base, 9, Font.ITALIC);
    }

    private static BaseFont loadBundledFont(String classpath) {
        try (InputStream is = CharacterPdfService.class.getResourceAsStream(classpath)) {
            if (is == null) return null;
            byte[] bytes = is.readAllBytes();
            return BaseFont.createFont(
                    classpath.substring(classpath.lastIndexOf('/') + 1),
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    false, bytes, null);
        } catch (Exception e) {
            log.debug("classpath:{} could not be loaded: {}", classpath, e.getMessage());
            return null;
        }
    }


    /**
     * Renders a character sheet to a PDF document.
     *
     * <p>Bundled DejaVu Sans fonts are loaded once at construction time and
     * embedded into the document so Cyrillic text renders on any host without
     * relying on system fonts. The template tree drives the layout: containers
     * become bold headings, field nodes become two-column rows of
     * label/value, separator/text nodes become inline blocks.
     *
     * @param character character whose field values are rendered (required)
     * @param template  template providing the field tree; when {@code null} or
     *                  empty a placeholder paragraph is emitted
     * @return rendered PDF bytes ready to stream back to the client
     * @throws IllegalStateException if {@code character} is {@code null} or if
     *         a PDF I/O error occurs during rendering
     */
    public byte[] generate(Character character, Template template) {
        if (character == null) {
            throw new IllegalStateException("Character is required for PDF generation");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            addHeader(doc, character, template);
            addMeta(doc, character, template);
            doc.add(new Paragraph(" ", regular));

            Paragraph charsTitle = new Paragraph("Характеристики", bold);
            charsTitle.setSpacingAfter(8f);
            doc.add(charsTitle);

            Map<String, Object> values = character.getFieldValues() != null
                    ? character.getFieldValues()
                    : Map.of();

            List<TemplateNode> content = (template != null && template.getContent() != null)
                    ? template.getContent()
                    : List.of();
            if (content.isEmpty()) {
                doc.add(new Paragraph("Шаблон не содержит полей.", small));
            } else {
                for (TemplateNode node : content) {
                    renderNode(doc, node, values, 0);
                }
            }
        } catch (Exception e) {
            log.error("PDF generation failed for character id={}",
                    character != null ? character.getId() : null, e);
            throw new IllegalStateException("Failed to generate PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }



    private void renderNode(Document doc, TemplateNode node,
                            Map<String, Object> values, int depth) throws Exception {
        switch (node) {
            case TemplateNode.ContainerNode container -> {
                Font containerFont = new Font(bold.getBaseFont(), 13, Font.BOLD);
                Paragraph title = new Paragraph(
                        nullSafe(container.title(), "Раздел"), containerFont);
                title.setSpacingBefore(10f);
                title.setSpacingAfter(6f);
                doc.add(title);
                for (TemplateNode child : container.children()) {
                    renderNode(doc, child, values, depth + 1);
                }
            }
            case TemplateNode.BlockNode block -> {
                Object val = values.get(block.id());
                doc.add(labelValue(nullSafe(block.label(), "Поле"),
                        val != null ? String.valueOf(val) : "—"));
            }
            case TemplateNode.CounterNode counter -> {
                Object val = values.get(counter.id());
                String shown = val != null ? String.valueOf(val)
                        : String.valueOf(counter.currentValue());
                if (counter.maxValue() != null) {
                    shown = shown + " / " + counter.maxValue();
                }
                doc.add(labelValue(nullSafe(counter.label(), "Счётчик"), shown));
            }
            case TemplateNode.TextFieldNode text -> {
                String val = values.get(text.id()) != null
                        ? String.valueOf(values.get(text.id())) : "";
                Paragraph p = new Paragraph();
                if (text.placeholder() != null && !text.placeholder().isBlank()) {
                    p.add(new Chunk(text.placeholder() + ":\n", bold));
                }
                p.add(new Chunk(val.isBlank() ? "(не заполнено)" : val, regular));
                p.setSpacingAfter(6f);
                doc.add(p);
            }
            case TemplateNode.TableNode table -> {
                Paragraph p = new Paragraph(
                        nullSafe(table.label(), "Таблица"), bold);
                p.setSpacingBefore(6f);
                p.setSpacingAfter(4f);
                doc.add(p);
                int rows = Math.max(table.rows(), 1);
                int cols = Math.max(table.columns(), 1);
                PdfPTable pdfTable = new PdfPTable(cols);
                pdfTable.setWidthPercentage(100f);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        String cellKey = table.id() + "_" + r + "_" + c;
                        Object cell = values.get(cellKey);
                        PdfPCell pdfCell = new PdfPCell(new Phrase(
                                cell != null ? String.valueOf(cell) : "", regular));
                        pdfCell.setPadding(4f);
                        pdfTable.addCell(pdfCell);
                    }
                }
                doc.add(pdfTable);
            }
        }
    }

    private Paragraph labelValue(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", bold));
        p.add(new Chunk(value, regular));
        p.setSpacingAfter(4f);
        return p;
    }

    private void addHeader(Document doc, Character c, Template t) throws Exception {
        Font titleFont = new Font(bold.getBaseFont(), 18, Font.BOLD);
        Paragraph title = new Paragraph(
                c.getName() != null ? c.getName() : "Персонаж", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8f);
        doc.add(title);

        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            Paragraph desc = new Paragraph(c.getDescription(), regular);
            desc.setAlignment(Element.ALIGN_CENTER);
            desc.setSpacingAfter(8f);
            doc.add(desc);
        }
    }

    private void addMeta(Document doc, Character c, Template t) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (t != null && t.getTitle() != null) {
            sb.append("Шаблон: ").append(t.getTitle()).append("    ");
        }
        if (c.getVisibility() != null) {
            sb.append("Видимость: ").append(c.getVisibility()).append("    ");
        }
        if (c.getCreatedAt() != null) {
            sb.append("Создан: ").append(c.getCreatedAt().format(DATE_FMT)).append("    ");
        }
        if (c.getUpdatedAt() != null) {
            sb.append("Обновлён: ").append(c.getUpdatedAt().format(DATE_FMT));
        }
        Paragraph meta = new Paragraph(sb.toString(), small);
        meta.setAlignment(Element.ALIGN_CENTER);
        doc.add(meta);
    }

    private static String nullSafe(String s, String dflt) {
        return s == null || s.isBlank() ? dflt : s;
    }



    private static BaseFont loadUnicodeBaseFont() {
        for (String path : FONT_CANDIDATES) {
            try {
                if (path != null && !path.toLowerCase().contains("bold")
                        && Files.exists(Path.of(path))) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception e) {
                log.debug("Шрифт {} не загружен: {}", path, e.getMessage());
            }
        }

        try (InputStream is = CharacterPdfService.class
                .getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return BaseFont.createFont(
                        "DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                        false, bytes, null);
            }
        } catch (IOException e) {
            log.debug("classpath:/fonts/DejaVuSans.ttf не найден: {}", e.getMessage());
        }
        log.warn("Не найден TTF‑шрифт с поддержкой кириллицы — использую Helvetica. "
                + "Кириллица может отображаться некорректно. Положите DejaVuSans.ttf "
                + "в src/main/resources/fonts/");
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI).getBaseFont();
    }

    private static BaseFont loadUnicodeBaseFontBold(BaseFont fallback) {
        for (String path : FONT_CANDIDATES) {
            try {
                if (path != null && path.toLowerCase().contains("bold")
                        && Files.exists(Path.of(path))) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {

            }
        }
        return fallback;
    }
}
