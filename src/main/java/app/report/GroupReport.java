package app.report;

import app.entities.Group;

import java.util.List;

public class GroupReport {

    private Group group;

    private List<WeeklyReport> weeklyReports;

    public GroupReport(Group group)
    {
        System.out.println("Processing " + group.getLeaderName() + "'s group.");
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<WeeklyReport> getWeeklyReports() {
        return weeklyReports;
    }

    public void setWeeklyReports(List<WeeklyReport> weeklyReports) {
        this.weeklyReports = weeklyReports;
    }
}
