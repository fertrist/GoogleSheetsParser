package app.generate;

import static app.extract.ReportUtil.containsIgnoreCase;
import app.data.GroupTableData;
import app.entities.Category;
import app.entities.Group;
import app.entities.Person;
import app.extract.WeeklyReportBuilder;
import app.report.GroupWeeklyReport;
import app.report.ReportItem;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class WeeklyReportGenerator
{
    private GroupTableData groupTableData;

    public WeeklyReportGenerator(GroupTableData groupTableData) {
        this.groupTableData = groupTableData;
    }

    public List<GroupWeeklyReport> generateWeeklyReportsForReportStartEnd(LocalDate reportStart, LocalDate reportEnd) throws IOException
    {
        BlankWeeklyReportGenerator blankWeeklyReportGenerator = new BlankWeeklyReportGenerator(groupTableData);

        List<GroupWeeklyReport> groupWeeklyReports = blankWeeklyReportGenerator.getWeeksBetweenStartEnd(reportStart, reportEnd);

        WeeklyReportBuilder weeklyReportBuilder = new WeeklyReportBuilder(groupTableData, groupWeeklyReports);
        groupWeeklyReports = weeklyReportBuilder.fillWeeksWithItems();

        GroupWeeklyReport lastGroupWeeklyReport = getLastWeeklyReport(groupWeeklyReports);
        adjustAddedRemovedPeople(lastGroupWeeklyReport);

        return groupWeeklyReports;
    }

    private void adjustAddedRemovedPeople(GroupWeeklyReport lastGroupWeeklyReport) {
        groupTableData.getPeople().forEach(person -> adjustPersonInWhiteList(person, lastGroupWeeklyReport.getWhiteList()));
        lastGroupWeeklyReport.getReportItems().forEach(this::adjustReportItem);
    }

    private void adjustReportItem(ReportItem reportItem)
    {
        Group group = groupTableData.getGroup();

        String personName = trimAndLowerCase(reportItem.getPerson().getName());

        if (toLowerCase(group.getAddedPeople()).contains(personName))
        {
            reportItem.getPerson().setCategory(Category.WHITE);
        }
        if (toLowerCase(group.getRemovedPeople()).contains(personName))
        {
            reportItem.getPerson().setCategory(Category.NEW);
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

    private GroupWeeklyReport getLastWeeklyReport(List<GroupWeeklyReport> groupWeeklyReports)
    {
        return groupWeeklyReports.get(groupWeeklyReports.size() - 1);
    }
}
