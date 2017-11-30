package app.entities;

import java.util.List;

public class GroupReport {

    private Group group;

    private List<GroupWeeklyReport> groupWeeklyReports;

    public GroupReport(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<GroupWeeklyReport> getGroupWeeklyReports() {
        return groupWeeklyReports;
    }

    public void setGroupWeeklyReports(List<GroupWeeklyReport> groupWeeklyReports) {
        this.groupWeeklyReports = groupWeeklyReports;
    }
}
