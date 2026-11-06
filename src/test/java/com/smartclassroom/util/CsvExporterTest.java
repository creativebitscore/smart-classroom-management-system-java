package com.smartclassroom.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExporterTest {

    @Test
    void plainTextIsNotQuoted() {
        assertEquals("hello", CsvExporter.escape("hello"));
    }

    @Test
    void commasQuotesAndLineBreaksAreEscaped() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"));
    }

    @Test
    void formulaCharactersAreNeutralised() {
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"));
    }

    @Test
    void fileContainsHeaderAndRows(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("out.csv");
        CsvExporter.export(file, List.of("Roll", "Name"), List.of(List.of("CSE1", "Aarav, M"), List.of("CSE2", "Priya")));
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("﻿Roll,Name"));
        assertTrue(text.contains("CSE1,\"Aarav, M\""));
        assertTrue(text.contains("CSE2,Priya"));
    }
}
