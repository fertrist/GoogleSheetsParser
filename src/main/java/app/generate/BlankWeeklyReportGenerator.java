package app.generate;

import static app.entities.Action.GROUP;
import static app.extract.ReportUtil.hasBackground;
import static app.extract.ReportUtil.isRowEmpty;
import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.entities.GroupMeetingNotes;
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

    public List<GroupWeeklyReport> getWeeksBetweenStartEnd(LocalDate start, LocalDate end)
    {
        List<GroupWeeklyReport> blankWeeklyReports = getWeeksBetweenStartEndDates(start, end);

        blankWeeklyReports.forEach(weeklyReport ->
        {
            GroupMeetingNotes groupMeetingNotes = getGroupMeetingNotes(weeklyReport.getGroupDate());
            weeklyReport.setPercents(groupMeetingNotes.getPercentage());
            weeklyReport.setGroupComments(groupMeetingNotes.getComments());
        });

        return blankWeeklyReports;
    }

    private GroupMeetingNotes getGroupMeetingNotes(LocalDate groupDate)
    {
        String note = getGroupNote(groupDate);
        return GroupMeetingNotes.fromNote(note);
    }

    private String getGroupNote(LocalDate groupDate)
    {
        int groupColumn = getColumnForGroupMeeting(groupDate);

        String note = null;

        if (groupColumn != -1)
        {
            note = getNoteFromDateOrDayCell(groupColumn);
        }
        return note;
    }

    private String getNoteFromDateOrDayCell(int groupColumn)
    {
        List<CellData> daysCells = getRowWithDays().getValues();
        List<CellData> datesCells = getRowWithDates().getValues();

        CellData dayCell = daysCells.get(groupColumn);
        CellData dateCell = datesCells.get(groupColumn);

        return dayCell.getNote() != null ? dayCell.getNote() : dateCell.getNote();
    }

    private int getColumnForGroupMeeting(LocalDate dateWhenGroupMeets)
    {
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();

        List<Integer> groupDayColumns = columnDateMapper.getColumnsFor(dateWhenGroupMeets);

        return getGroupDayColumn(groupDayColumns);
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

    private List<GroupWeeklyReport> getWeeksBetweenStartEndDates(LocalDate start, LocalDate end)
    {
        List<GroupWeeklyReport> groupWeeklyReports = new ArrayList<>();

        for (LocalDate tmp = start; tmp.isBefore(end) || tmp.isEqual(end); tmp = tmp.plusWeeks(1))
        {
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
