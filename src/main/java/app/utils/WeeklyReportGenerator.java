package app.utils;

import static app.enums.Actions.GROUP;
import static app.utils.Configuration.getReportEndDate;
import static app.utils.Configuration.getReportStartDate;
import static app.utils.ReportUtil.areColorsEqual;
import static app.utils.ReportUtil.containsIgnoreCase;
import static app.utils.ReportUtil.getActionByColor;
import static app.utils.ReportUtil.getWeeksFromDates;
import static app.utils.ReportUtil.hasBackground;
import static app.utils.ReportUtil.isRowEmpty;
import app.GroupTableData;
import app.dao.PeopleAndColorExtractor;
import app.entities.Group;
import app.entities.GroupWeeklyReport;
import app.entities.Person;
import app.entities.ReportItem;
import app.enums.Actions;
import app.enums.Category;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;
import javafx.util.Pair;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeeklyReportGenerator
{
    private PeopleAndColorExtractor peopleAndColorExtractor;
    private GroupTableData groupTableData;

    public WeeklyReportGenerator(GroupTableData groupTableData) {
        this.groupTableData = groupTableData;
    }

    public List<GroupWeeklyReport> generateWeeklyReports() throws IOException
    {
        List<Person> people = peopleAndColorExtractor.getPeople();

        Map<Actions, Color> colors = peopleAndColorExtractor.extractColors();

        List<RowData> data = groupTableData.getData();

        ColumnToDateMapper columnToDateMapper = groupTableData.getColumnToDateMapper();

        List<GroupWeeklyReport> groupWeeklyReports = getWeeks(data, groupTableData.getGroup(), columnToDateMapper, colors.get(GROUP));

        List<ReportItem> reportItems = getItems(people, colors, columnToDateMapper.getReportColumns(), data, columnToDateMapper);

        groupWeeklyReports = fillWeeks(reportItems, people, groupWeeklyReports);

        handleAddedRemovedToList(groupWeeklyReports, groupTableData.getGroup(), people);

        return groupWeeklyReports;
    }

    private static void handleAddedRemovedToList(List<GroupWeeklyReport> groupWeeklyReports, Group group, List<Person> people) {

        GroupWeeklyReport groupWeeklyReport = groupWeeklyReports.get(groupWeeklyReports.size() - 1);

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

    private static List<GroupWeeklyReport> fillWeeks(List<ReportItem> reportItems, List<Person> people, List<GroupWeeklyReport> groupWeeklyReports) {

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

    private static boolean withinStartEnd(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isAfter(start) || date.isEqual(start)) && (date.isBefore(end) || date.isEqual(end));
    }

    private static List<ReportItem> getItems(List<Person> people, Map<Actions, Color> colors,
                                             Pair<Integer, Integer> startEndColumns,
                                             List<RowData> dataRows, ColumnToDateMapper columnToDateMapper)
    {
        List<ReportItem> reportItems = new ArrayList<>();
        int diff = startEndColumns.getValue() - startEndColumns.getKey();
        for (Person person : people)
        {
            // case where row is empty for the person thus not fetched
            if (dataRows.size() < person.getIndex()+1)
            {
                continue;
            }
            RowData row = dataRows.get(person.getIndex());

            if (isRowEmpty(row)) continue;

            List<CellData> personCells = row.getValues();
            /*List<CellData> personCells = row.getValues().subList(0, diff).stream().filter(ReportUtil::hasBackground)
                    .collect(Collectors.toList());*/

            for (int i = 0; i < personCells.size(); i++)
            {
                CellData cell = personCells.get(i);

                if (!hasBackground(cell)) continue;

                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();

                Actions action = getActionByColor(bgColor, colors);

                if (action != null)
                {
                    LocalDate date = columnToDateMapper.dateForColumn(i);
                    reportItems.add(new ReportItem(person.clone(), action, date));
                }
            }
        }
        return reportItems;
    }

    private List<GroupWeeklyReport> getWeeks(List<RowData> rows, Group group, ColumnToDateMapper columnToDateMapper,
                                             Color groupColor) {
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

            List<Integer> groupDayColumns = columnToDateMapper.getColumnsForDate(groupDate);

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

                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
                if (areColorsEqual(bgColor, groupColor)) {
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

    public void setPeopleAndColorExtractor(PeopleAndColorExtractor peopleAndColorExtractor) {
        this.peopleAndColorExtractor = peopleAndColorExtractor;
    }
}
