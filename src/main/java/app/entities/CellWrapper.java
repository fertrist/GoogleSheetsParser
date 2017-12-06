package app.entities;

import app.conf.Constants;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ExtendedValue;

public class CellWrapper
{
    private CellData cellData;

    public CellWrapper(CellData cellData)
    {
        this.cellData = cellData;
    }

    public boolean isColorsTitle() {
        return isUnderline() && cellData.getEffectiveValue() != null
                && cellData.getEffectiveValue().getStringValue().equalsIgnoreCase(Constants.COLORS_TITLE);
    }

    public boolean isUnderline() {
        return cellData.getEffectiveFormat() != null && cellData.getEffectiveFormat().getTextFormat().getUnderline();
    }

    public boolean isCellEmpty() {
        return cellData.getEffectiveValue() == null || cellData.getEffectiveValue().getStringValue() == null
                || cellData.getEffectiveValue().getStringValue().isEmpty();
    }

    public String getStringValue()
    {
        ExtendedValue effectiveValue = cellData.getEffectiveValue();
        return effectiveValue != null ? effectiveValue.getStringValue() : null;
    }

    public ColorWrapper getBgColor()
    {
        CellFormat effectiveFormat = cellData.getEffectiveFormat();
        Color color = effectiveFormat != null ? effectiveFormat.getBackgroundColor() : null;
        return new ColorWrapper(color);

    }
}
