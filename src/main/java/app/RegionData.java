package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}
