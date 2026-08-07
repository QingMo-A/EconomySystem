package com.mo.economy_system.ui.geometry;

/** Immutable rectangle in the common virtual UI coordinate space. */
public record UiRect(int x, int y, int width, int height) {
    public UiRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("UI rectangle dimensions cannot be negative");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }

    public boolean contains(UiRect other) {
        return other != null && other.x >= x && other.y >= y
                && other.right() <= right() && other.bottom() <= bottom();
    }

    public boolean overlaps(UiRect other) {
        return other != null && x < other.right() && right() > other.x
                && y < other.bottom() && bottom() > other.y;
    }

    public UiRect inset(UiInsets insets) {
        int nextWidth = Math.max(0, width - insets.left() - insets.right());
        int nextHeight = Math.max(0, height - insets.top() - insets.bottom());
        return new UiRect(x + insets.left(), y + insets.top(), nextWidth, nextHeight);
    }
}
