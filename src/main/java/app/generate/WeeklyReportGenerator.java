package app.generate;

import static app.conf.Configuration.getReportEndDate;
import static app.conf.Configuration.getReportStartDate;
import static app.entities.Action.GROUP;
import static app.extract.ReportUtil.containsIgnoreCase;
import static app.extract.ReportUtil.getWeeksFromDates;
import static app.extract.ReportUtil.hasBackground;
import static app.extract.ReportUtil.isRowEmpty;
import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.Category;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.entities.Group;
import app.entities.Person;
import app.extract.ItemsExtractor;
import app.report.GroupWeeklyReport;
import app.report.ReportItem;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WeeklyReportGenerator
{
    private GroupTableData groupTableData;

    private WeeklyReportGenerator(Builder builder) {
        this.groupTableData = builder.groupTableData;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public List<GroupWeeklyReport> generateWeeklyReports() throws IOException
    {
        List<ReportItem> reportItems = new ItemsExtractor(groupTableData).getItems();

        List<GroupWeeklyReport> groupWeeklyReports = getWeeks();

        groupWeeklyReports = fillWeeks(reportItems, groupWeeklyReports);

        handleAddedRemovedToList(groupWeeklyReports);

        return groupWeeklyReports;
    }

    private void handleAddedRemovedToList(List<GroupWeeklyReport> groupWeeklyReports) {

        GroupWeeklyReport groupWeeklyReport = groupWeeklyReports.get(groupWeeklyReports.size() - 1);

        Group group = groupTableData.getGroup();
        List<Person> people = groupTableData.getPeople();

        for (Person person : people)
        {
            if (containsIgnoreCase(group.getAddedPeople(), person.getName())) {
                Person updated = person.clone();
                updated.setCategory(Category.WHITE);
                groupWeeklyReport.getWhiteList().add(updated);
            }
            else if (containsIgnoreCase(group.getRemovedPeople(), person.getName())) {
                Person updated = person.clone();
                groupWeeklyReport.getWhiteList().remove(updated);
                updated.setCategory(Category.NEW);
            }
        }
        for (ReportItem reportItem : groupWeeklyReport.getReportItems()) {
            if (containsIgnoreCase(group.getAddedPeople(), reportItem.getPerson().getName())) {
                reportItem.getPerson().setCategory(Category.WHITE);
            }
            if (containsIgnoreCase(group.getRemovedPeople(), reportItem.getPerson().getName())) {
                reportItem.getPerson().setCategory(Category.NEW);
            }
        }
    }

    private List<GroupWeeklyReport> fillWeeks(List<ReportItem> reportItems, List<GroupWeeklyReport> groupWeeklyReports) {

        List<Person> people = groupTableData.getPeople();
        groupWeeklyReports.sort(Comparator.comparing(GroupWeeklyReport::getStart));
        List<Person> whiteList = people.stream().filter(p -> p.getCategory() == Category.WHITE)
                .map(Person::clone).collect(Collectors.toList());
        groupWeeklyReports.forEach(week -> week.getWhiteList().addAll(whiteList));
        groupWeeklyReports.forEach(
                w -> w.getReportItems().addAll(reportItems.stream()
                                .filter(i -> withinStartEnd(i.getDate(), w.getStart(), w.getEnd())).collect(Collectors.toList())
                ));
        return groupWeeklyReports;
    }

    private boolean withinStartEnd(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isAfter(start) || date.isEqual(start)) && (date.isBefore(end) || date.isEqual(end));
    }

    private List<GroupWeeklyReport> getWeeks()
    {
        ColorActionMapper colorActionMapper = groupTableData.getColorActionMapper();
        Color groupColor = colorActionMapper.getColorForAction(GROUP);
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();
        List<RowData> rows = groupTableData.getData();
        Group group = groupTableData.getGroup();

        Integer groupDay = group.getGroupDay().ordinal();
        LocalDate reportStart = LocalDate.parse(getReportStartDate());
        LocalDate reportEnd = LocalDate.parse(getReportEndDate());

        RowData daysRow = rows.get(group.getRowWithDays());
        List<CellData> daysCells = daysRow.getValues();

        RowData datesRow = rows.get(group.getRowWithDates());
        List<CellData> datesCells = datesRow.getValues();

        List<GroupWeeklyReport> groupWeeklyReports = getWeeksFromDates(reportStart, reportEnd);

        for (GroupWeeklyReport groupWeeklyReport : groupWeeklyReports) {

            LocalDate groupDate = groupWeeklyReport.getStart().plusDays(groupDay - 1);

            List<Integer> groupDayColumns = columnDateMapper.getColumnsForDate(groupDate);

            int groupColumn = getGroupDayColumn(groupDayColumns, rows, groupColor);

            if (groupColumn == -1) continue;

            CellData dayCell = daysCells.get(groupColumn);
            CellData dateCell = datesCells.get(groupColumn);

            String groupNote = dayCell.getNote();
            groupNote = groupNote != null ? groupNote : dateCell.getNote();

            setGroupComments(groupWeeklyReport, groupNote);
        }
        return groupWeeklyReports;
    }

    private static int getGroupDayColumn(List<Integer> groupDayColumns, List<RowData> rows, Color groupColor) {
        for (RowData row : rows) {

            if (isRowEmpty(row)) continue;

            for (int column : groupDayColumns) {
                if (column > row.getValues().size()) continue;

                CellData cell = row.getValues().get(column);

                if (!hasBackground(cell)) continue;

                ColorWrapper bgColor = new CellWrapper(cell).getBgColor();
                if (bgColor.equals(new ColorWrapper(groupColor))) {
                    return column;
                }
            }
        }
        return -1;
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

    public static class Builder
    {
        private GroupTableData groupTableData;

        public Builder withGroupTableData(GroupTableData groupTableData) {
            this.groupTableData = groupTableData;
            return this;
        }

        public WeeklyReportGenerator build()
        {
            return new WeeklyReportGenerator(this);
        }
    }
}
