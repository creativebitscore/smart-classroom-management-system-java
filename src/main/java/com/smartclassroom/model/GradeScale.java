package com.smartclassroom.model;

/**
 * 10-point grading scale used by most Indian universities: percentage -> letter grade -> grade points.
 * An enum with a lookup method, so the rule lives in exactly one place.
 */
public enum GradeScale {
    O("O", 90, 10),
    A_PLUS("A+", 80, 9),
    A("A", 70, 8),
    B_PLUS("B+", 60, 7),
    B("B", 50, 6),
    C("C", 45, 5),
    P("P", 40, 4),
    F("F", 0, 0);

    private final String letter;
    private final double minPercentage;
    private final int points;

    GradeScale(String letter, double minPercentage, int points) {
        this.letter = letter;
        this.minPercentage = minPercentage;
        this.points = points;
    }

    public String getLetter() {
        return letter;
    }

    public int getPoints() {
        return points;
    }

    public boolean isPass() {
        return this != F;
    }

    /** The grade for a percentage (values below 0 count as F, values above 100 as O). */
    public static GradeScale forPercentage(double percentage) {
        for (GradeScale g : values()) {
            if (percentage >= g.minPercentage) {
                return g;
            }
        }
        return F;
    }

    @Override
    public String toString() {
        return letter;
    }
}
