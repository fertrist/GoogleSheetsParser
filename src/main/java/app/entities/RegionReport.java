package app.entities;

import app.enums.Category;

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
            for (GroupWeeklyReport groupWeeklyReport : groupReport.getGroupWeeklyReports()) {
                innerSet.addAll(groupWeeklyReport.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
            }
            list.addAll(innerSet);
        }
        return list.size();
    }

    public int getTotalWhiteCount() {
        int whiteCount = 0;
        for (GroupReport groupReport : groupReports) {
            whiteCount += groupReport.getGroupWeeklyReports().get(groupReport.getGroupWeeklyReports().size()-1).getWhiteList().size();
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