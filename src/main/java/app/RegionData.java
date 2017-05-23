package app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionData {

    private String leader;
    private List<Map<String, List<Week>>> groups;

    public RegionData(String leader) {
        this.leader = leader;
        groups = new ArrayList<>();
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public List<Map<String, List<Week>>> getGroups() {
        return groups;
    }

    public void setGroups(List<Map<String, List<Week>>> groups) {
        this.groups = groups;
    }

    public int getTotalNewCount() {
        List<String> list = new ArrayList<>();
        for (Map<String, List<Week>> group : groups) {
            Set<String> innerSet = new HashSet<>();
            Map.Entry<String, List<Week>> entry = group.entrySet().iterator().next();
            for (Week week : entry.getValue()) {
                innerSet.addAll(week.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
            }
            list.addAll(innerSet);
        }
        return list.size();
    }

    public int getTotalWhiteCount() {
        int whiteCount = 0;
        for (Map<String, List<Week>> group : groups) {
            Map.Entry<String, List<Week>> entry = group.entrySet().iterator().next();
            whiteCount += entry.getValue().get(entry.getValue().size()-1).getWhiteList().size();
        }
        return whiteCount;
    }
}
