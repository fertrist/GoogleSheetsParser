package app.report;

import app.entities.Person;
import app.entities.Action;

import java.time.LocalDate;

public class ReportItem {

    private Action action;
    private Person person;
    private LocalDate date;

    public ReportItem(Action action, LocalDate date) {
        this.action = action;
        this.date = date;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Person getPerson() {
        return person;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "ReportItem{" +
                "action=" + action +
                ", person=" + person +
                ", date='" + date + '\'' +
                '}';
    }

    public boolean isWithinWeekDateRange(GroupWeeklyReport weeklyReport)
    {
        LocalDate start = weeklyReport.getStart();
        LocalDate end = weeklyReport.getEnd();
        return (date.isAfter(start) || date.isEqual(start)) && (date.isBefore(end) || date.isEqual(end));
    }
}
