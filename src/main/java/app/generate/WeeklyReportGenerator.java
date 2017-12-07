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
}
