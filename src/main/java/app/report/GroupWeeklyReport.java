package app.report;

import app.entities.Action;
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
    private List<ReportItem> reportItems = new ArrayList<>();

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
        return reportItems.stream().filter(i -> i.getAction() == Action.GROUP)
                .map(ReportItem::getPerson).collect(Collectors.toList());
    }

    public int getTotalCount() {
        return getPresent().size();
    }

    public List<Person> getPresentByCategory(Category... categories)
    {
        return reportItems.stream().filter(i -> i.getAction() == Action.GROUP && Arrays.asList(categories).contains(i.getPerson().getCategory()))
                .map(ReportItem::getPerson).collect(Collectors.toList());
    }

    public List<Person> getWhiteAbsent()
    {
        List<Person> whiteCopy = new ArrayList<>(whiteList);
        whiteCopy.removeAll(getPresentByCategory(Category.WHITE));
        return whiteCopy;
    }

    public int getMeetingNew()
    {
        return getItemsByActionAndCategory(Action.MEETING, Category.NEW).size();
    }

    private List<ReportItem> getItemsByActionAndCategory(Action action, Category... categories)
    {
        return reportItems.stream().filter(i -> i.getAction() == action
                && Arrays.asList(categories).contains(i.getPerson().getCategory())).collect(Collectors.toList());
    }

    public LocalDate getGroupDate()
    {
        int groupDayOrdinal = groupDay.ordinal();
        return getStart().plusDays(groupDayOrdinal - 1);
    }

    public int getMeetingWhite()
    {
        return getItemsByActionAndCategory(Action.MEETING, Category.WHITE, Category.TRIAL).size();
    }

    public int getVisitNew()
    {
      return getItemsByActionAndCategory(Action.VISIT, Category.NEW).size();
    }

    public int getVisitWhite()
    {
      return getItemsByActionAndCategory(Action.VISIT, Category.WHITE, Category.TRIAL).size();
    }

    public int getCalls() {
        return getItemsByActionAndCategory(Action.CALL, Category.NEW).size();
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

    public List<ReportItem> getReportItems() {
        return reportItems;
    }

    public void setReportItems(List<ReportItem> reportItems)
    {
        this.reportItems.addAll(reportItems);
    }
}
