package app.generate;

import static app.extract.ReportUtil.containsIgnoreCase;
import app.dao.GroupSheetApi;
import app.data.GroupTableData;
import app.entities.Category;
import app.entities.Group;
import app.entities.Person;
import app.extract.WeeklyReportBuilder;
import app.report.Event;
import app.report.WeeklyReport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class WeeklyReportGenerator
{
    private GroupTableData groupTableData;
    private GroupSheetApi groupSheetApi;

    public WeeklyReportGenerator(GroupSheetApi groupSheetApi)
    {
        this.groupSheetApi = groupSheetApi;
    }

    public List<WeeklyReport> generateWeeklyReportsForReportStartEnd(LocalDate reportStart, LocalDate reportEnd) throws IOException
    {
        groupTableData = groupSheetApi.getGroupTableData();

        BlankWeeklyReportGenerator blankWeeklyReportGenerator = new BlankWeeklyReportGenerator(groupTableData);

        List<WeeklyReport> weeklyReports = blankWeeklyReportGenerator.getWeeksBetweenStartEnd(reportStart, reportEnd);

        WeeklyReportBuilder weeklyReportBuilder = new WeeklyReportBuilder(groupTableData, weeklyReports);
        weeklyReports = weeklyReportBuilder.fillWeeksWithItems();

        WeeklyReport lastWeeklyReport = getLastWeeklyReport(weeklyReports);
        adjustAddedRemovedPeople(lastWeeklyReport);

        return weeklyReports;
    }

    private void adjustAddedRemovedPeople(WeeklyReport lastWeeklyReport) {
        groupTableData.getPeople()
                .forEach(person -> adjustPersonInWhiteList(person, lastWeeklyReport.getWhiteList()));

        lastWeeklyReport.getEvents()
                .forEach(this::adjustReportItem);
    }

    private void adjustReportItem(Event event)
    {
        Group group = groupTableData.getGroup();

        String personName = trimAndLowerCase(event.getPerson().getName());

        if (toLowerCase(group.getAddedPeople()).contains(personName))
        {
            event.getPerson().setCategory(Category.WHITE);
        }
        if (toLowerCase(group.getRemovedPeople()).contains(personName))
        {
            event.getPerson().setCategory(Category.NEW);
        }
    }

    private void adjustPersonInWhiteList(Person person, List<Person> whiteList)
    {
        Group group = groupTableData.getGroup();

        Person updated = person.clone();

        Category category = null;
        if (containsIgnoreCase(group.getAddedPeople(), person.getName()))
        {
            category = Category.WHITE;
            whiteList.add(updated);
        }
        else if (containsIgnoreCase(group.getRemovedPeople(), person.getName()))
        {
            whiteList.remove(updated);
            category = Category.NEW;
        }
        updated.setCategory(category);
    }

    private List<String> toLowerCase(List<String> stringList)
    {
        return stringList.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    private String trimAndLowerCase(String string)
    {
        return string.replaceAll("\\s+", " ").replaceAll("\\(.*\\)|\\d", "").trim();

    }

    private WeeklyReport getLastWeeklyReport(List<WeeklyReport> weeklyReports)
    {
        return weeklyReports.get(weeklyReports.size() - 1);
    }
}
