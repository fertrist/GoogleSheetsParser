package app;

import java.util.ArrayList;
import java.util.List;

public class Week {
    private int weekNumber;

    private int meetingNew = 0;
    private int meetingWhite = 0;
    private int visitNew = 0;
    private int visitWhite = 0;

    private List<Person> whiteList = new ArrayList<>();
    private List<Person> present = new ArrayList<>();

    public Week(int weekNumber, List<Person> whiteList)
    {
        this.weekNumber = weekNumber;
        this.whiteList = whiteList;
    }

    public String getWeekName() {
        return "Week-" + weekNumber;
    }

    public void addPresent(Person person) {
        this.present.add(person);
    }

    public void mergeAction(Marks action, Category category) {
        if (category == Category.WHITE) {
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
        }
    }

    public int countPresentByCategory(Category category)
    {
        return (int) present.stream().filter(p -> p.getCategory() == category).count();
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

    @Override
    public String toString() {
        return "Week{" +
                "weekNumber=" + weekNumber +
                ", meetingNew=" + meetingNew +
                ", meetingWhite=" + meetingWhite +
                ", visitNew=" + visitNew +
                ", visitWhite=" + visitWhite +
                ", whiteList=" + whiteList +
                ", present=" + present +
                '}';
    }
}
