package com.smartclassroom.util;

import com.smartclassroom.service.exception.AppException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes rows to a CSV file that opens correctly in Excel (UTF-8 with BOM, quotes escaped). */
public final class CsvExporter {
    private CsvExporter() {
    }

    public static void export(Path file, List<String> headers, List<List<String>> rows) {
        try (OutputStream out = Files.newOutputStream(file);
             BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.write('﻿'); // BOM so Excel detects UTF-8
            writer.write(toLine(headers));
            for (List<String> row : rows) {
                writer.write(toLine(row));
            }
        } catch (IOException e) {
            throw new AppException("Could not write the CSV file: " + e.getMessage(), e);
        }
    }

    static String toLine(List<String> cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(cells.get(i)));
        }
        return line.append("\r\n").toString();
    }

    /** Wraps a cell in quotes when it contains a comma, quote or line break; doubles any quotes inside. */
    static String escape(String cell) {
        if (cell == null) {
            return "";
        }
        // guard against spreadsheet formula injection (cells starting with = + - @)
        String safe = cell;
        if (!safe.isEmpty() && "=+@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
