package app.generate;

import static app.conf.Configuration.getReportEndDate;
import static app.conf.Configuration.getReportStartDate;
import static app.extract.ReportUtil.containsIgnoreCase;
import app.data.GroupTableData;
import app.entities.Category;
import app.entities.Group;
import app.entities.Person;
import app.extract.ItemsExtractor;
import app.report.GroupWeeklyReport;
import app.report.ReportItem;

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

        BlankWeeklyReportGenerator blankWeeklyReportGenerator = new BlankWeeklyReportGenerator(groupTableData);

        LocalDate reportStart = LocalDate.parse(getReportStartDate());
        LocalDate reportEnd = LocalDate.parse(getReportEndDate());
        List<GroupWeeklyReport> groupWeeklyReports = blankWeeklyReportGenerator.getWeeksForDates(reportStart, reportEnd);

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
