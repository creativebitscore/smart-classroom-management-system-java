package com.smartclassroom.util;

import com.smartclassroom.service.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void emptyAndBlankTextIsRejected() {
        assertThrows(ValidationException.class, () -> Validator.requireText("", "Name", 50));
        assertThrows(ValidationException.class, () -> Validator.requireText("   ", "Name", 50));
        assertThrows(ValidationException.class, () -> Validator.requireText(null, "Name", 50));
    }

    @Test
    void textIsTrimmedAndLengthChecked() {
        assertEquals("Aarav", Validator.requireText("  Aarav ", "Name", 50));
        assertThrows(ValidationException.class, () -> Validator.requireText("abcdef", "Name", 5));
    }

    @Test
    void errorMessageNamesTheField() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> Validator.requireText("", "Roll number", 20));
        assertTrue(e.getMessage().contains("Roll number"));
    }

    @Test
    void emailFormatIsChecked() {
        assertEquals("a@b.com", Validator.requireEmail("a@b.com"));
        assertThrows(ValidationException.class, () -> Validator.requireEmail("not-an-email"));
        assertThrows(ValidationException.class, () -> Validator.requireEmail("a@b"));
        assertThrows(ValidationException.class, () -> Validator.requireEmail(""));
    }

    @Test
    void phoneIsOptionalButMustBeTenDigits() {
        assertNull(Validator.optionalPhone(""));
        assertNull(Validator.optionalPhone(null));
        assertEquals("9876543210", Validator.optionalPhone("9876543210"));
        assertThrows(ValidationException.class, () -> Validator.optionalPhone("12345"));
        assertThrows(ValidationException.class, () -> Validator.optionalPhone("98765abcde"));
    }

    @Test
    void usernameRules() {
        assertEquals("nisha.gupta", Validator.requireUsername("nisha.gupta"));
        assertThrows(ValidationException.class, () -> Validator.requireUsername("ab"));
        assertThrows(ValidationException.class, () -> Validator.requireUsername("has space"));
    }

    @Test
    void passwordNeedsEightCharactersWithLettersAndDigits() {
        assertEquals("Strong@123", Validator.requirePassword("Strong@123"));
        assertThrows(ValidationException.class, () -> Validator.requirePassword("short1"));
        assertThrows(ValidationException.class, () -> Validator.requirePassword("onlyletters"));
        assertThrows(ValidationException.class, () -> Validator.requirePassword("12345678"));
        assertThrows(ValidationException.class, () -> Validator.requirePassword(null));
    }

    @Test
    void numbersAndRanges() {
        assertEquals(5, Validator.parseInt(" 5 ", "Capacity"));
        assertThrows(ValidationException.class, () -> Validator.parseInt("abc", "Capacity"));
        assertThrows(ValidationException.class, () -> Validator.parseInt("", "Capacity"));
        assertEquals(2.5, Validator.parseDouble("2.5", "Marks"), 0.0001);
        assertThrows(ValidationException.class, () -> Validator.parseDouble("x", "Marks"));
        assertEquals(3, Validator.requireIntInRange(3, "Semester", 1, 8));
        assertThrows(ValidationException.class, () -> Validator.requireIntInRange(9, "Semester", 1, 8));
        assertThrows(ValidationException.class, () -> Validator.requireIntInRange(0, "Semester", 1, 8));
    }
}
