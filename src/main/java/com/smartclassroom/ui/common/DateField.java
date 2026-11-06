package com.smartclassroom.ui.common;

import com.smartclassroom.service.exception.ValidationException;

import javax.swing.JTextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Text field for dates typed as yyyy-MM-dd. */
public class DateField extends JTextField {
    private static final long serialVersionUID = 1L;

    public DateField() {
        super(10);
        putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
    }

    public DateField(LocalDate initial) {
        this();
        setDate(initial);
    }

    public void setDate(LocalDate date) {
        setText(date == null ? "" : date.toString());
    }

    /** Required date; throws ValidationException when empty or badly formatted. */
    public LocalDate getDate(String fieldName) {
        LocalDate date = getOptionalDate(fieldName);
        if (date == null) {
            throw new ValidationException(fieldName + " is required (format yyyy-MM-dd).");
        }
        return date;
    }

    /** Optional date; returns null when the box is empty. */
    public LocalDate getOptionalDate(String fieldName) {
        String text = getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must look like 2026-11-15 (yyyy-MM-dd).");
        }
    }
}
