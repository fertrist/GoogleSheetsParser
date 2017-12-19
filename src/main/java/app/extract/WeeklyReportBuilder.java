package app.extract;

import app.data.GroupTableData;
import app.report.Event;
import app.report.GroupWeeklyReport;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WeeklyReportBuilder
{
    private GroupTableData groupTableData;
    private List<GroupWeeklyReport> groupWeeklyReports;

    public WeeklyReportBuilder(GroupTableData groupTableData, List<GroupWeeklyReport> groupWeeklyReports)
    {
        this.groupTableData = groupTableData;
        this.groupWeeklyReports = groupWeeklyReports;
    }

    public List<GroupWeeklyReport> fillWeeksWithItems()
    {
        updateWeeksWithWhiteList();

        List<Event> events = new EventsExtractor(groupTableData).extractEvents();

        updateWeeksWithItems(events);

        return groupWeeklyReports;
    }

    private void updateWeeksWithWhiteList()
    {
        groupWeeklyReports.forEach(week -> week.getWhiteList().addAll(groupTableData.getWhiteList()));
    }

    private List<GroupWeeklyReport> updateWeeksWithItems(List<Event> events)
    {
        groupWeeklyReports.forEach(weeklyReport ->
        {
            List<Event> weeklyEvents = filterWeeklyReportItems(events, withinWeekBounds(weeklyReport));
            weeklyReport.setEvents(weeklyEvents);
        });
        return groupWeeklyReports;
    }

    private List<Event> filterWeeklyReportItems(List<Event> events, Predicate<Event> filter)
    {
        return events.stream().filter(filter).collect(Collectors.toList());
    }

    private Predicate<Event> withinWeekBounds(GroupWeeklyReport weeklyReport) {
        return reportItem -> reportItem.isWithinWeekDateRange(weeklyReport);
    }
}
