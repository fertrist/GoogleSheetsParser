package app.extract;

import app.data.GroupTableData;
import app.report.Event;
import app.report.WeeklyReport;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WeeklyReportBuilder
{
    private GroupTableData groupTableData;
    private List<WeeklyReport> weeklyReports;

    public WeeklyReportBuilder(GroupTableData groupTableData, List<WeeklyReport> weeklyReports)
    {
        this.groupTableData = groupTableData;
        this.weeklyReports = weeklyReports;
    }

    public List<WeeklyReport> fillWeeksWithItems()
    {
        updateWeeksWithWhiteList();

        List<Event> events = new EventsExtractor(groupTableData).extractEvents();

        updateWeeksWithItems(events);

        return weeklyReports;
    }

    private void updateWeeksWithWhiteList()
    {
        weeklyReports.forEach(week -> week.getWhiteList().addAll(groupTableData.getWhiteList()));
    }

    private List<WeeklyReport> updateWeeksWithItems(List<Event> events)
    {
        weeklyReports.forEach(weeklyReport ->
        {
            List<Event> weeklyEvents = filterWeeklyReportItems(events, withinWeekBounds(weeklyReport));
            weeklyReport.setEvents(weeklyEvents);
        });
        return weeklyReports;
    }

    private List<Event> filterWeeklyReportItems(List<Event> events, Predicate<Event> filter)
    {
        return events.stream().filter(filter).collect(Collectors.toList());
    }

    private Predicate<Event> withinWeekBounds(WeeklyReport weeklyReport) {
        return reportItem -> reportItem.isWithinWeekDateRange(weeklyReport);
    }
}
