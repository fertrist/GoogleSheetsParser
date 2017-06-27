package app.entities;

import app.enums.Category;
import app.enums.Actions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Week {

    private LocalDate start;
    private LocalDate end;
    private String percents;
    private String groupComments;

    private List<Person> whiteList = new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    public Week() {}

    public List<Person> getWhiteList() {
        return whiteList;
    }

    public List<Person> getPresent() {
        return items.stream().filter(i -> i.getAction() == Actions.GROUP)
                .map(Item::getPerson).collect(Collectors.toList());
    }

    public int getTotalCount() {
        return getPresent().size();
    }

    public List<Person> getPresentByCategory(Category category)
    {
        return items.stream().filter(i -> i.getAction() == Actions.GROUP && i.getPerson().getCategory() == category)
                .map(Item::getPerson).collect(Collectors.toList());
    }

    public List<Person> getWhiteAbsent()
    {
        List<Person> whiteCopy = new ArrayList<>(whiteList);
        whiteCopy.removeAll(getPresentByCategory(Category.WHITE));
        return whiteCopy;
    }

    public int getMeetingNew()
    {
        return getItemsByActionAndCategory(Actions.MEETING, Category.NEW).size();
    }

    private List<Item> getItemsByActionAndCategory(Actions action, Category category)
    {
        return items.stream().filter(i -> i.getAction() == action
                && i.getPerson().getCategory() == category).collect(Collectors.toList());
    }


    public int getMeetingWhite()
    {
        return getItemsByActionAndCategory(Actions.MEETING, Category.WHITE).size();
    }

    public int getVisitNew()
    {
      return getItemsByActionAndCategory(Actions.VISIT, Category.NEW).size();
    }

    public int getVisitWhite()
    {
      return getItemsByActionAndCategory(Actions.VISIT, Category.WHITE).size();
    }

    public int getCalls() {
        return getItemsByActionAndCategory(Actions.CALL, Category.NEW).size();
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

    public List<Item> getItems() {
        return items;
    }
}
