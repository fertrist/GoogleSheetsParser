package app.entities;

import app.enums.Category;

import java.util.*;
import java.util.stream.Collectors;

public class Region {

    private String leader;
    private Map<Group, List<Week>> groups;

    public Region(String leader) {
        this.leader = leader;
        groups = new TreeMap<>();
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public Map<Group, List<Week>> getGroups() {
        return groups;
    }

    public void setGroups(Map<Group, List<Week>> groups) {
        this.groups = groups;
    }

    public int getTotalNewCount() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<Group, List<Week>> entry : groups.entrySet()) {
            Set<String> innerSet = new HashSet<>();
            for (Week week : entry.getValue()) {
                innerSet.addAll(week.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
            }
            list.addAll(innerSet);
        }
        return list.size();
    }

    public int getTotalWhiteCount() {
        int whiteCount = 0;
        for (Map.Entry<Group, List<Week>> entry : groups.entrySet()) {
            whiteCount += entry.getValue().get(entry.getValue().size()-1).getWhiteList().size();
        }
        return whiteCount;
    }
}
