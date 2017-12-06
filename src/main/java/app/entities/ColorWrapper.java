package app.entities;

import com.google.api.services.sheets.v4.model.Color;

import java.util.Objects;

public class ColorWrapper
{
    private Color color;

    public ColorWrapper(Color color) {
        this.color = color;
    }

    public Color getColor(int red, int green, int blue) {
        Color color = new Color();
        color.setRed((float) (red / 255.0));
        color.setGreen((float) (green / 255.0));
        color.setBlue((float) (blue / 255.0));
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorWrapper that = (ColorWrapper) o;

        return (color == that.getColor()) || (color != null) && Objects.equals(color.getBlue(), that.getColor().getBlue()) && Objects.equals(color.getRed(), that.getColor().getRed()) && Objects.equals(color.getGreen(), that.getColor().getGreen());
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(color);
    }

    public boolean isWhite() {
        return color != null && color.getBlue() != null && color.getBlue() == 1.0
                && color.getGreen() != null && color.getGreen() == 1.0
                && color.getRed() != null && color.getRed() == 1.0;
    }

    public boolean isGrey() {
        return !isWhite() && color.getBlue() != null && color.getGreen() != null && color.getRed() != null
                && color.getBlue().equals(color.getGreen()) && color.getGreen().equals(color.getRed());
    }

    public Color getColor() {
        return color;
    }

    public boolean isPresent()
    {
        return color != null;
    }
}
