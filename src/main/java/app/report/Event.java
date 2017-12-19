package app.report;

import app.entities.EventType;
import app.entities.Person;

import java.time.LocalDate;

public class Event {

    private EventType eventType;
    private Person person;
    private LocalDate date;

    public Event(EventType eventType) {
        this.eventType = eventType;
    }

    public Event(EventType eventType, LocalDate date) {
        this.eventType = eventType;
        this.date = date;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
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
        return "Event{" +
                "eventType=" + eventType +
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
