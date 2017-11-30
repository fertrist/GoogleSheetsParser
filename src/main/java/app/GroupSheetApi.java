package app;

import app.data.CustomSheetApi;
import app.entities.Group;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;

import java.io.IOException;
import java.util.List;

public class GroupSheetApi
{
    private static CustomSheetApi sheetApi;

    private Group group;

    private Sheet sheet;

    public GroupSheetApi(Group group)
    {
        this.group = group;
    }

    public Sheet getGroupSheet()
    {
        String spreadsheetId = group.getSpreadSheetId();

        if (sheet != null)
        {
            return sheet;
        }
        try
        {
            return sheetApi.getSheet(spreadsheetId, group.getRowWithMonths(), group.getDataFirstRow() - 1);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public List<GridRange> getMergesWithMonths()
    {
        return getGroupSheet().getMerges();
    }

    public List<CellData> getCellsWithMonths()
    {
        RowData monthsRow = getSheetRows().get(toIndex(group.getRowWithMonths()));
        return monthsRow.getValues();
    }

    public List<CellData> getCellsWithDates()
    {
        RowData datesRow = getSheetRows().get(toIndex(group.getRowWithDates()));
        return datesRow.getValues();
    }

    private List<RowData> getSheetRows()
    {
        return sheet.getData().get(0).getRowData();
    }

    public static void setSheetApi(CustomSheetApi sheetApi)
    {
        GroupSheetApi.sheetApi = sheetApi;
    }

    private static int toIndex(int value)
    {
        return value - 1;
    }

}
