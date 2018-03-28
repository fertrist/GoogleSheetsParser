package app.generate;

import static app.conf.Configuration.getReportEndDate;
import static app.conf.Configuration.getReportStartDate;
import app.dao.GroupSheetApi;
import app.entities.Group;
import app.report.GroupReport;
import app.report.WeeklyReport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class GroupReportGenerator
{
    private Group group;

    public GroupReportGenerator(Group group) {
        this.group = group;
    }

    public GroupReport generateGroupReport()
    {
        GroupReport groupReport = new GroupReport(group);
        try
        {
            List<WeeklyReport> weeklyReports = generateWeeklyReports();
            groupReport.setWeeklyReports(weeklyReports);
        }
        catch (IOException e)
        {
            throw new RuntimeException();
        }
        return groupReport;
    }

    private List<WeeklyReport> generateWeeklyReports() throws IOException
    {
        LocalDate reportStart = LocalDate.parse(getReportStartDate());
        LocalDate reportEnd = LocalDate.parse(getReportEndDate());
        return generateWeeklyReports(reportStart, reportEnd);
    }

    private List<WeeklyReport> generateWeeklyReports(LocalDate reportStart, LocalDate reportEnd) throws IOException
    {
        GroupSheetApi groupSheetApi = new GroupSheetApi(group);
        WeeklyReportGenerator weeklyReportGenerator = new WeeklyReportGenerator(groupSheetApi);
        return weeklyReportGenerator.generateWeeklyReportsForReportStartEnd(reportStart, reportEnd);
    }

}
