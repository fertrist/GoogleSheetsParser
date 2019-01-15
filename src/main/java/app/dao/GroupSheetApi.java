package app.dao;

import static app.extract.ReportUtil.isRowEmpty;
import app.data.GroupTableData;
import app.entities.CellWrapper;
import app.entities.Group;
import app.extract.ColorExtractor;
import app.extract.PeopleExtractor;
import app.extract.ReportUtil;
import app.report.ReportRange;
import com.google.api.services.sheets.v4.model.CellData;
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
            sheet = sheetApi.getSheet(spreadsheetId, group.getRowWithMonths(), group.getDataFirstRow() - 1);
            return sheet;
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
        groupTableData.setData(getDataRows(groupTableData.getReportRange()));
        groupTableData.setPeople(new PeopleExtractor(this).extractPeople());
        groupTableData.setColorActionMapper(new ColorExtractor(this).getColorActionMapper());
        return groupTableData;
    }

    public List<RowData> getDataRows(ReportRange reportRange) throws IOException {

        reportRange = updateReportRange(reportRange);

        return sheetApi.getRowsData(group.getSpreadSheetId(), reportRange);
    }

    private ReportRange updateReportRange(ReportRange reportRange) throws IOException
    {
        Pair<Integer, Integer> dataColorRows = getLastDataAndColorsRow(getRowsWithPeopleAndColors());
        int lastDataRow = dataColorRows.getKey();
        reportRange.getStart().setRow(group.getRowWithMonths());
        reportRange.getEnd().setRow(lastDataRow);
        return reportRange;
    }

    public Pair<Integer, Integer> getLastDataAndColorsRow(List<RowData> rows) {
        int lastDataRow = 0;
        int colorsRow = 0;

        int offsetFromStart = group.getDataFirstRow();

        for (int i = offsetFromStart; i < rows.size(); i++) {

            RowData r = rows.get(i);

            if (isRowEmpty(r)) continue;

            CellData cell = r.getValues().get(Math.min(r.getValues().size() - 1, 1));
            CellWrapper cellWrapper = new CellWrapper(cell);

            if (cellWrapper.isCellEmpty()) continue;

            if (cellWrapper.isColorsTitle()) {
                colorsRow = i;
                break;
            }

            lastDataRow = i;
        }
        return new Pair<>(lastDataRow + offsetFromStart, colorsRow + offsetFromStart);
    }

    public List<RowData> getRowsWithPeopleAndColors() throws IOException
    {
        String rangeExpression = sheetApi.getRangeExpression(group.getDataFirstRow(), ReportUtil.MAX_ROWS, group.getPeopleColumnAsNumber(), group.getPeopleColumn());
        return sheetApi.getRowsData(group.getSpreadSheetId(), rangeExpression);
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
        return getGroupSheet().getData().get(0).getRowData();
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
