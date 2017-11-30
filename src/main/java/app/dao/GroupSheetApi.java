package app.dao;

import app.GroupTableData;
import app.entities.Group;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import javafx.util.Pair;

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

    public GroupTableData getGroupTableData()
    {
        GroupTableData groupTableData = new GroupTableData();
        groupTableData.setDatesRow(getDatesRow());
        groupTableData.setMerges(getMonthMerges());
        groupTableData.setMonthsRow(getMonthsRow());
        // TODO groupTableData.setData();
        return groupTableData;
    }

    public List<RowData> getData(Pair<Integer, Integer> reportColumns) throws IOException {
        int dataOffset = group.getDataFirstRow();

        PeopleAndColorExtractor peopleAndColorExtractor = new PeopleAndColorExtractor(group);

        Pair<Integer, Integer> dataColorRows = PeopleAndColorExtractor.getLastDataAndColorsRow(peopleAndColorExtractor.getRowsWithPeopleAndColors(), dataOffset);

        int lastDataRow = dataColorRows.getKey();

        return sheetApi.getRowsData(group.getSpreadSheetId(),
                group.getRowWithMonths(), lastDataRow, reportColumns.getKey(), reportColumns.getValue());
    }

    public List<GridRange> getMonthMerges()
    {
        return getGroupSheet().getMerges();
    }

    public RowData getMonthsRow()
    {
        int rowWithMonths = toIndex(group.getRowWithMonths());
        return getSheetRows().get(rowWithMonths);
    }

    public RowData getDatesRow()
    {
        int rowWithDates = toIndex(group.getRowWithDates());
        return getSheetRows().get(rowWithDates);
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
