package app.entities;

import app.enums.Category;
import app.enums.Marks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Week {

    private String leader;
    private int weekNumber;
    private int meetingNew = 0;
    private int meetingWhite = 0;
    private int visitNew = 0;
    private int visitWhite = 0;
    private int calls = 0;
    private String start;
    private String end;
    private String percents;
    private String groupComments;

    private List<Person> whiteList = new ArrayList<>();
    private List<Person> present = new ArrayList<>();

    public Week() {}

    public Week(String leader, int weekNumber, List<Person> whiteList)
    {
        this.leader = leader;
        this.weekNumber = weekNumber;
        this.whiteList = whiteList;
    }

    public String getWeekName() {
        return start + "-" + end;
    }

    public void addPresent(Person person) {
        this.present.add(person);
    }

    public void mergeAction(Marks action, Category category) {
        if (category == Category.WHITE || category == Category.GUEST) {
            if (action == Marks.MEETING) {
                increaseMeetingWhite();
            }
            if (action == Marks.VISIT) {
                increaseVisitWhite();
            }
        }
        if (category == Category.NEW) {
            if (action == Marks.MEETING) {
                increaseMeetingNew();
            }
            if (action == Marks.VISIT) {
                increaseVisitNew();
            }
            if (action == Marks.CALL) {
                increaseCalls();
            }
        }
    }

    public List<Person> getWhiteList() {
        return whiteList;
    }

    public List<Person> getPresent() {
        return present;
    }

    public int getTotalCount() {
        return present.size();
    }

    public int countPresentByCategory(Category category)
    {
        return (int) present.stream().filter(p -> p.getCategory() == category).count();
    }

    public List<Person> getPresentByCategory(Category category)
    {
        return present.stream().filter(p -> p.getCategory() == category).collect(Collectors.toList());
    }

    public List<Person> getWhiteAbsent() {
        List<Person> whiteCopy = new ArrayList<>(whiteList);
        whiteCopy.removeAll(present);
        return whiteCopy;
    }

    public int getMeetingNew()
    {
      return meetingNew;
    }

    public void increaseMeetingNew()
    {
      this.meetingNew++;
    }

    public int getMeetingWhite()
    {
      return meetingWhite;
    }

    public void increaseMeetingWhite()
    {
      this.meetingWhite++;
    }

    public int getVisitNew()
    {
      return visitNew;
    }

    public void increaseVisitNew()
    {
      this.visitNew++;
    }

    public void increaseCalls()
    {
        this.calls++;
    }

    public int getVisitWhite()
    {
      return visitWhite;
    }

    public void increaseVisitWhite()
    {
      this.visitWhite++;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public int getCalls() {
        return calls;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public void setMeetingNew(int meetingNew) {
        this.meetingNew = meetingNew;
    }

    public void setMeetingWhite(int meetingWhite) {
        this.meetingWhite = meetingWhite;
    }

    public void setVisitNew(int visitNew) {
        this.visitNew = visitNew;
    }

    public void setVisitWhite(int visitWhite) {
        this.visitWhite = visitWhite;
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }

    public void setWhiteList(List<Person> whiteList) {
        this.whiteList = whiteList;
    }

    public void setPresent(List<Person> present) {
        this.present = present;
    }

    public String getStart() {
        return start;
    }

    public void setStart(int month, int day) {
        this.start = String.format("%02d.%02d", day, month);
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(int month, int day) {
        this.end = String.format("%02d.%02d", day, month);
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getPercents() {
        return percents;
    }

    public void setPercents(String percents) {
        this.percents = percents;
    }

    public String getGroupComments() {
        return groupComments;
    }

    public void setGroupComments(String groupComments) {
        this.groupComments = groupComments;
    }

}
