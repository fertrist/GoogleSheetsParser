package app.extract;

import app.data.GroupTableData;
import app.report.GroupWeeklyReport;
import app.report.ReportItem;

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

        List<ReportItem> reportItems = new ReportItemsExtractor(groupTableData).extractItems();

        updateWeeksWithItems(reportItems);

        return groupWeeklyReports;
    }

    private void updateWeeksWithWhiteList()
    {
        groupWeeklyReports.forEach(week -> week.getWhiteList().addAll(groupTableData.getWhiteList()));
    }

    private List<GroupWeeklyReport> updateWeeksWithItems(List<ReportItem> reportItems)
    {
        groupWeeklyReports.forEach(weeklyReport ->
        {
            List<ReportItem> weeklyReportItems = filterWeeklyReportItems(reportItems, withinWeekBounds(weeklyReport));
            weeklyReport.setReportItems(weeklyReportItems);
        });
        return groupWeeklyReports;
    }

    private List<ReportItem> filterWeeklyReportItems(List<ReportItem> reportItems, Predicate<ReportItem> filter)
    {
        return reportItems.stream().filter(filter).collect(Collectors.toList());
    }

    private Predicate<ReportItem> withinWeekBounds(GroupWeeklyReport weeklyReport) {
        return reportItem -> reportItem.isWithinWeekDateRange(weeklyReport);
    }
}
