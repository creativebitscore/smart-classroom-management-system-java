package com.smartclassroom.util;

import com.smartclassroom.service.exception.ValidationException;

import java.util.regex.Pattern;

/** Reusable input checks. Every method throws {@link ValidationException} with a message the user can understand. */
public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.]{3,30}$");

    private Validator() {
    }

    /** Returns the trimmed text, or throws if it is null/blank or longer than maxLength. */
    public static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    public static String requireEmail(String value) {
        String email = requireText(value, "Email", 100);
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Enter a valid email address (for example name@college.edu).");
        }
        return email;
    }

    /** Phone is optional; if present it must be exactly 10 digits. Returns null for empty input. */
    public static String optionalPhone(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String phone = value.trim();
        if (!PHONE.matcher(phone).matches()) {
            throw new ValidationException("Phone number must be exactly 10 digits.");
        }
        return phone;
    }

    public static String requireUsername(String value) {
        String username = requireText(value, "Username", 30);
        if (!USERNAME.matcher(username).matches()) {
            throw new ValidationException("Username must be 3-30 characters: letters, digits, dot or underscore only.");
        }
        return username;
    }

    /** Password rule: at least 8 characters with at least one letter and one digit. */
    public static String requirePassword(String value) {
        if (value == null || value.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("Password must contain both letters and digits.");
        }
        return value;
    }

    public static int requireIntInRange(int value, String fieldName, int min, int max) {
        if (value < min || value > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    /** Parses a whole number typed in a text field. */
    public static int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a whole number.");
        }
    }

    /** Parses a decimal number typed in a text field. */
    public static double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a number.");
        }
    }

    public static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new ValidationException("Please select " + fieldName + ".");
        }
        return value;
    }
}
