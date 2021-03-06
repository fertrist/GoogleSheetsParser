package app.entities;

import app.enums.Category;

public class Person implements Cloneable {

    private String name;
    private Category category;
    private int index;

    public Person(Category category, String name, int index) {
        this.category = category;
        this.name = name;
        this.index = index;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (index != person.index) return false;
        if (name != null ? !name.equals(person.name) : person.name != null) return false;
        return category == person.category;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + index;
        return result;
    }

    public Person clone() {
        return new Person(this.category, this.getName(), this.getIndex());
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", category=" + category +
                ", index=" + index +
                '}';
    }
}
