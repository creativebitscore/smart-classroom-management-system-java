package com.smartclassroom.ui;

import com.smartclassroom.model.Admin;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every sidebar entry of every role must open a screen (and no screen may be left without a menu entry). */
class PanelFactoryTest {

    private static void assertMenuMatchesScreens(User user) {
        List<String> menu = user.getMenuItems();
        assertEquals(menu.size(), new HashSet<>(menu).size(), "menu names must be unique");
        // the screens are only created when clicked, so this does not need a display or a database
        assertEquals(new HashSet<>(menu), new HashSet<>(PanelFactory.screensFor(user).keySet()));
    }

    @Test
    void adminMenuMatchesScreens() {
        assertMenuMatchesScreens(new Admin());
    }

    @Test
    void facultyMenuMatchesScreens() {
        assertMenuMatchesScreens(new Faculty());
    }

    @Test
    void studentMenuMatchesScreens() {
        assertMenuMatchesScreens(new Student());
    }

    @Test
    void newFeaturesAreOnTheRightMenus() {
        assertTrue(new Student().getMenuItems().containsAll(List.of(Student.MENU_GRADE_CARD, Student.MENU_LEAVE)));
        assertTrue(new Faculty().getMenuItems().containsAll(
                List.of(Faculty.MENU_RANKINGS, Faculty.MENU_LEAVE_REQUESTS, Faculty.MENU_ROOM_BOOKING)));
        assertTrue(new Admin().getMenuItems().containsAll(
                List.of(Admin.MENU_RANKINGS, Admin.MENU_LEAVE_REQUESTS, Admin.MENU_ROOM_REQUESTS)));
    }
}
