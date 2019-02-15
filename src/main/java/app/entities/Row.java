package app.entities;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;

import java.util.List;

public class Row
{
    private List<CellData> rowValues;

    public Row(RowData rowData)
    {
        this.rowValues = rowData.getValues();
    }

    public String getValue(int column)
    {
        return rowValues.get(column).getEffectiveValue().getStringValue();
    }
}
