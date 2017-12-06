package app.generate;

import app.dao.GroupSheetApi;
import app.entities.Group;
import app.report.GroupReport;
import app.report.GroupWeeklyReport;

import java.io.IOException;
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
        return generateWeeklyReports();
    }

    private List<GroupWeeklyReport> generateWeeklyReports() throws IOException
    {
        GroupSheetApi groupSheetApi = new GroupSheetApi(group);

        WeeklyReportGenerator weeklyReportGenerator = WeeklyReportGenerator.builder()
                .withGroupTableData(groupSheetApi.getGroupTableData())
                .build();

        return weeklyReportGenerator.generateWeeklyReports();

    }

}
