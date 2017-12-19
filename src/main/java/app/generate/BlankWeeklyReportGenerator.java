package app.generate;

import static app.entities.EventType.GROUP;
import static app.extract.ReportUtil.hasBackground;
import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.entities.GroupMeetingNotes;
import app.extract.ReportUtil;
import app.report.GroupWeeklyReport;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BlankWeeklyReportGenerator
{
    private GroupTableData groupTableData;

    public BlankWeeklyReportGenerator(GroupTableData groupTableData)
    {
        this.groupTableData = groupTableData;
    }

    public List<GroupWeeklyReport> getWeeksBetweenStartEnd(LocalDate start, LocalDate end)
    {
        List<GroupWeeklyReport> blankWeeklyReports = getBlankWeeksBetweenStartEndDates(start, end);

        blankWeeklyReports.forEach(weeklyReport ->
        {
            Optional<GroupMeetingNotes> groupMeetingNotes = getGroupMeetingNotes(weeklyReport.getGroupDate());
            groupMeetingNotes.ifPresent(notes ->
            {
                weeklyReport.setPercents(notes.getPercentage());
                weeklyReport.setGroupComments(notes.getComments());
            });
        });

        return blankWeeklyReports;
    }

    private Optional<GroupMeetingNotes> getGroupMeetingNotes(LocalDate groupDate)
    {
        String note = getGroupNote(groupDate);
        return Optional.ofNullable(note != null ? GroupMeetingNotes.fromNote(note) : null);
    }

    private String getGroupNote(LocalDate groupDate)
    {
        Integer groupColumn = getGroupColumn(groupDate);
        return groupColumn != null ? getNoteFromGroupColumn(groupColumn) : null;
    }

    private String getNoteFromGroupColumn(int groupColumn)
    {
        List<CellData> daysCells = getRowWithDays().getValues();
        List<CellData> datesCells = getRowWithDates().getValues();

        CellData dayCell = daysCells.get(groupColumn);
        CellData dateCell = datesCells.get(groupColumn);

        return dayCell.getNote() != null ? dayCell.getNote() : dateCell.getNote();
    }

    private Integer getGroupColumn(LocalDate groupDate)
    {
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();
        List<Integer> columnsForGroupDate = columnDateMapper.getColumnsFor(groupDate);

        return getGroupColumn(columnsForGroupDate);
    }

    private Integer getGroupColumn(List<Integer> groupDayColumns) {

        List<RowData> notEmptyRows = groupTableData.getData().stream().filter(ReportUtil::isRowEmpty).collect(Collectors.toList());

        Integer groupColumn = null;

        for (RowData row : notEmptyRows)
        {
            groupColumn = getGroupDayColumnByBackGround(row, groupDayColumns);
        }
        return groupColumn;
    }

    private Integer getGroupDayColumnByBackGround(RowData row, List<Integer> groupDayColumns)
    {
        for (Integer column : groupDayColumns)
        {
            if (column > row.getValues().size()) continue;

            CellData cell = row.getValues().get(column);

            if (!hasBackground(cell)) continue;

            ColorWrapper bgColor = new CellWrapper(cell).getBgColor();
            if (bgColor.equals(getGroupActionColor()))
            {
                return column;
            }
        }
        return null;
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

    private ColorWrapper getGroupActionColor()
    {
        ColorActionMapper colorActionMapper = groupTableData.getColorActionMapper();
        return new ColorWrapper(colorActionMapper.getColorForAction(GROUP));
    }

    private List<GroupWeeklyReport> getBlankWeeksBetweenStartEndDates(LocalDate start, LocalDate end)
    {
        List<GroupWeeklyReport> groupWeeklyReports = new ArrayList<>();

        for (LocalDate tmp = start; tmp.isBefore(end) || tmp.isEqual(end); tmp = tmp.plusWeeks(1))
        {
            GroupWeeklyReport groupWeeklyReport = new GroupWeeklyReport();
            groupWeeklyReport.setStart(tmp);
            groupWeeklyReport.setEnd(tmp.plusDays(6));
            groupWeeklyReports.add(groupWeeklyReport);
            groupWeeklyReport.setGroupDay(groupTableData.getGroup().getGroupDay());
        }
        return groupWeeklyReports;
    }
}
