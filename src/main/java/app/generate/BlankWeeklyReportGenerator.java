package app.generate;

import static app.entities.Action.GROUP;
import static app.extract.ReportUtil.hasBackground;
import static app.extract.ReportUtil.isRowEmpty;
import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.report.GroupWeeklyReport;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BlankWeeklyReportGenerator
{
    private GroupTableData groupTableData;

    public BlankWeeklyReportGenerator(GroupTableData groupTableData)
    {
        this.groupTableData = groupTableData;
    }

    public List<GroupWeeklyReport> getWeeksForDates(LocalDate start, LocalDate end) {
        List<GroupWeeklyReport> groupWeeklyReports = getWeeksFromDates(start, end);
        groupWeeklyReports.forEach(this::setCommentsAndNotes);
        return groupWeeklyReports;
    }


    private void setCommentsAndNotes(GroupWeeklyReport groupWeeklyReport)
    {
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();



        LocalDate groupDate = groupWeeklyReport.getGroupDate();

        List<Integer> groupDayColumns = columnDateMapper.getColumnsForDate(groupDate);

        int groupColumn = getGroupDayColumn(groupDayColumns);

        if (groupColumn != -1)
        {
            List<CellData> daysCells = getRowWithDays().getValues();
            List<CellData> datesCells = getRowWithDates().getValues();

            CellData dayCell = daysCells.get(groupColumn);
            CellData dateCell = datesCells.get(groupColumn);

            String groupNote = dayCell.getNote();
            groupNote = groupNote != null ? groupNote : dateCell.getNote();

            setGroupComments(groupWeeklyReport, groupNote);
        }


    }

    private RowData getRowWithDays()
    {
        int rowWithDays = groupTableData.getGroup().getRowWithDays();
        return groupTableData.getData().get(rowWithDays);
    }

    private RowData getRowWithDates()
    {
        int rowWithDates = groupTableData.getGroup().getRowWithDates();
        return groupTableData.getData().get(rowWithDates);
    }

    private Color getGroupActionColor()
    {
        ColorActionMapper colorActionMapper = groupTableData.getColorActionMapper();
        return colorActionMapper.getColorForAction(GROUP);
    }

    private static void setGroupComments(GroupWeeklyReport groupWeeklyReport, String groupNote) {

        if (groupNote != null && !groupNote.isEmpty()) {
            String firstString = groupNote.split("\\n")[0];
            if (firstString.matches("[0-9]+[%]")) {
                groupWeeklyReport.setPercents(firstString);
                String comment = groupNote
                        .substring(groupNote.indexOf(firstString), groupNote.length());
                groupWeeklyReport.setGroupComments(comment.trim());
            } else {
                groupWeeklyReport.setGroupComments(groupNote);
            }
        }
    }

    private List<GroupWeeklyReport> getWeeksFromDates(LocalDate start, LocalDate end) {
        List<GroupWeeklyReport> groupWeeklyReports = new ArrayList<>();
        for (LocalDate tmp = start; tmp.isBefore(end) || tmp.isEqual(end); tmp = tmp.plusWeeks(1)) {
            GroupWeeklyReport groupWeeklyReport = new GroupWeeklyReport();
            groupWeeklyReport.setStart(tmp);
            groupWeeklyReport.setEnd(tmp.plusDays(6));
            groupWeeklyReports.add(groupWeeklyReport);
        }
        return groupWeeklyReports;
    }

    private int getGroupDayColumn(List<Integer> groupDayColumns) {

        for (RowData row : groupTableData.getData()) {

            if (isRowEmpty(row)) continue;

            for (int column : groupDayColumns) {
                if (column > row.getValues().size()) continue;

                CellData cell = row.getValues().get(column);

                if (!hasBackground(cell)) continue;

                ColorWrapper bgColor = new CellWrapper(cell).getBgColor();
                if (bgColor.equals(new ColorWrapper(getGroupActionColor()))) {
                    return column;
                }
            }
        }
        return -1;
    }
}
