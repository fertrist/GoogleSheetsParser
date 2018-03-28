package app.report;

import app.entities.Person;
import app.entities.Category;

import java.util.*;
import java.util.stream.Collectors;

public class RegionReport {

    private String leader;
    private List<GroupReport> groupReports;

    public RegionReport(String leader) {
        this.leader = leader;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public int getTotalNewCount() {
        List<String> list = new ArrayList<>();
        for (GroupReport groupReport : groupReports) {
            Set<String> innerSet = new HashSet<>();
            for (WeeklyReport weeklyReport : groupReport.getWeeklyReports()) {
                innerSet.addAll(weeklyReport.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
            }
            list.addAll(innerSet);
        }
        return list.size();
    }

    public int getTotalWhiteCount() {
        int whiteCount = 0;
        for (GroupReport groupReport : groupReports) {
            whiteCount += groupReport.getWeeklyReports().get(groupReport.getWeeklyReports().size()-1).getWhiteList().size();
        }
        return whiteCount;
    }

    public List<GroupReport> getGroupReports() {
        return groupReports;
    }

    public void setGroupReports(List<GroupReport> groupReports) {
        this.groupReports = groupReports;
    }
}
