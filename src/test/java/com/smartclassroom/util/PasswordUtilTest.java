package com.smartclassroom.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void correctPasswordVerifies() {
        String hash = PasswordUtil.hash("Admin@123");
        assertTrue(PasswordUtil.verify("Admin@123", hash));
    }

    @Test
    void wrongPasswordIsRejected() {
        String hash = PasswordUtil.hash("Admin@123");
        assertFalse(PasswordUtil.verify("admin@123", hash));
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfRandomSalt() {
        assertNotEquals(PasswordUtil.hash("Student@123"), PasswordUtil.hash("Student@123"));
    }

    @Test
    void hashDoesNotContainThePlainPassword() {
        assertFalse(PasswordUtil.hash("Student@123").contains("Student@123"));
    }

    @Test
    void malformedOrPlaceholderHashNeverVerifies() {
        assertFalse(PasswordUtil.verify("anything", "PENDING"));
        assertFalse(PasswordUtil.verify("anything", "a:b"));
        assertFalse(PasswordUtil.verify("anything", null));
        assertFalse(PasswordUtil.verify(null, PasswordUtil.hash("x1234567")));
    }
}
