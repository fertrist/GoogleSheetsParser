package app.entities;

import app.enums.Actions;

import java.time.LocalDate;

public class Item {

    private Actions action;
    private Person person;
    private LocalDate date;

    public Item(Person person, Actions action, LocalDate date) {
        this.action = action;
        this.date = date;
        this.person = person;
    }

    public Actions getAction() {
        return action;
    }

    public void setAction(Actions action) {
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
        return "Item{" +
                "action=" + action +
                ", person=" + person +
                ", date='" + date + '\'' +
                '}';
    }
}
