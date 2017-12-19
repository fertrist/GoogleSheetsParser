package app.report;

import app.entities.EventType;
import app.entities.Category;
import app.entities.Person;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GroupWeeklyReport {

    private LocalDate start;
    private LocalDate end;
    private String percents;
    private String groupComments;
    private DayOfWeek groupDay;

    private List<Person> whiteList = new ArrayList<>();
    private List<Event> events = new ArrayList<>();

    public GroupWeeklyReport() {}

    public DayOfWeek getGroupDay() {
        return groupDay;
    }

    public void setGroupDay(DayOfWeek groupDay) {
        this.groupDay = groupDay;
    }

    public List<Person> getWhiteList() {
        return whiteList;
    }

    public List<Person> getPresent() {
        return events.stream().filter(i -> i.getEventType() == EventType.GROUP)
                .map(Event::getPerson).collect(Collectors.toList());
    }

    public int getTotalCount() {
        return getPresent().size();
    }

    public List<Person> getPresentByCategory(Category... categories)
    {
        return events.stream().filter(i -> i.getEventType() == EventType.GROUP && Arrays.asList(categories).contains(i.getPerson().getCategory()))
                .map(Event::getPerson).collect(Collectors.toList());
    }

    public List<Person> getWhiteAbsent()
    {
        List<Person> whiteCopy = new ArrayList<>(whiteList);
        whiteCopy.removeAll(getPresentByCategory(Category.WHITE));
        return whiteCopy;
    }

    public int getMeetingNew()
    {
        return getItemsByActionAndCategory(EventType.MEETING, Category.NEW).size();
    }

    private List<Event> getItemsByActionAndCategory(EventType eventType, Category... categories)
    {
        return events.stream().filter(i -> i.getEventType() == eventType
                && Arrays.asList(categories).contains(i.getPerson().getCategory())).collect(Collectors.toList());
    }

    public LocalDate getGroupDate()
    {
        int groupDayOrdinal = groupDay.ordinal();
        return getStart().plusDays(groupDayOrdinal - 1);
    }

    public int getMeetingWhite()
    {
        return getItemsByActionAndCategory(EventType.MEETING, Category.WHITE, Category.TRIAL).size();
    }

    public int getVisitNew()
    {
      return getItemsByActionAndCategory(EventType.VISIT, Category.NEW).size();
    }

    public int getVisitWhite()
    {
      return getItemsByActionAndCategory(EventType.VISIT, Category.WHITE, Category.TRIAL).size();
    }

    public int getCalls() {
        return getItemsByActionAndCategory(EventType.CALL, Category.NEW).size();
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public void setEnd(LocalDate end) {
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

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events)
    {
        this.events.addAll(events);
    }
}
