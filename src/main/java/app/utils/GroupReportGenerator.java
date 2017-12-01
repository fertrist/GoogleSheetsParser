package app.utils;

import app.GroupTableData;
import app.dao.GroupSheetApi;
import app.dao.PeopleAndColorExtractor;
import app.entities.Group;
import app.entities.GroupReport;
import app.entities.GroupWeeklyReport;

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

        GroupTableData groupTableData = new GroupSheetApi(group).getGroupTableData();

        return generateWeeklyReports(groupTableData);
    }

    private List<GroupWeeklyReport> generateWeeklyReports(GroupTableData groupTableData) throws IOException
    {
        PeopleAndColorExtractor peopleAndColorExtractor = new PeopleAndColorExtractor(groupTableData.getGroup());

        WeeklyReportGenerator weeklyReportGenerator = new WeeklyReportGenerator(groupTableData);
        weeklyReportGenerator.setPeopleAndColorExtractor(peopleAndColorExtractor);

        return weeklyReportGenerator.generateWeeklyReports();

    }

}
