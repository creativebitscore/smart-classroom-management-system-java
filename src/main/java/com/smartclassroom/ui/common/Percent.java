package com.smartclassroom.ui.common;

/** A percentage that prints as "83.3%" yet sorts by its number inside a table. */
public final class Percent implements Comparable<Percent> {
    private final double value;

    public Percent(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(Percent other) {
        return Double.compare(value, other.value);
    }

    @Override
    public String toString() {
        return String.format("%.1f%%", value);
    }
}
