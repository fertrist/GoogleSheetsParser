package app.generate;

import static app.conf.Configuration.getReportEndDate;
import static app.conf.Configuration.getReportStartDate;
import app.dao.GroupSheetApi;
import app.entities.Group;
import app.report.GroupReport;
import app.report.GroupWeeklyReport;

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
            List<GroupWeeklyReport> groupWeeklyReports = generateGroupWeeklyReports();
            groupReport.setGroupWeeklyReports(groupWeeklyReports);
        }
        catch (IOException e)
        {
            throw new RuntimeException();
        }
        return groupReport;
    }

    private List<GroupWeeklyReport> generateGroupWeeklyReports() throws IOException
    {
        System.out.println("Processing " + group.getLeaderName() + "'s group.");
        LocalDate reportStart = LocalDate.parse(getReportStartDate());
        LocalDate reportEnd = LocalDate.parse(getReportEndDate());
        return generateWeeklyReports(reportStart, reportEnd);
    }

    private List<GroupWeeklyReport> generateWeeklyReports(LocalDate reportStart, LocalDate reportEnd) throws IOException
    {
        GroupSheetApi groupSheetApi = new GroupSheetApi(group);
        WeeklyReportGenerator weeklyReportGenerator = new WeeklyReportGenerator(groupSheetApi.getGroupTableData());
        return weeklyReportGenerator.generateWeeklyReportsForReportStartEnd(reportStart, reportEnd);
    }

}
