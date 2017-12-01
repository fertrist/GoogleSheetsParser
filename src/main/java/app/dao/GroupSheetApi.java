package app.dao;

import app.GroupTableData;
import app.entities.Group;
import app.entities.ReportRange;
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

    public GroupTableData getGroupTableData() throws IOException
    {
        GroupTableData groupTableData = new GroupTableData();
        groupTableData.setGroup(group);
        groupTableData.setDatesRow(getDatesRow());
        groupTableData.setMerges(getMonthMerges());
        groupTableData.setMonthsRow(getMonthsRow());
        groupTableData.initColumnToDateMapper();
        groupTableData.initReportLimit();
        groupTableData = setData(groupTableData);
        return groupTableData;
    }

    public GroupTableData setData(GroupTableData groupTableData) throws IOException {

        ReportRange reportRange = groupTableData.getReportRange();

        reportRange = updateReportRange(reportRange);

        List<RowData> data = sheetApi.getRowsData(group.getSpreadSheetId(), reportRange);

        groupTableData.setData(data);

        return groupTableData;
    }

    private ReportRange updateReportRange(ReportRange reportRange) throws IOException
    {
        PeopleAndColorExtractor peopleAndColorExtractor = new PeopleAndColorExtractor(group);

        Pair<Integer, Integer> dataColorRows = peopleAndColorExtractor
                .getLastDataAndColorsRow(peopleAndColorExtractor.getRowsWithPeopleAndColors());

        int lastDataRow = dataColorRows.getKey();

        reportRange.getStart().setRow(group.getRowWithMonths());

        reportRange.getEnd().setRow(lastDataRow);

        return reportRange;
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

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
}
