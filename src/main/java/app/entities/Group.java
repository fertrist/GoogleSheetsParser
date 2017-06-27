package app.entities;

import com.google.common.base.Objects;

import java.time.DayOfWeek;
import java.util.List;

public class Group implements Comparable<Group> {

    private int groupNumber;
    private String spreadSheetId;
    private String leaderName;
    private int rowWithMonths;
    private int rowWithDates;
    private int rowWithDays;
    private DayOfWeek groupDay;
    private int dataFirstRow;
    private String peopleColumn;
    private List<String> addedPeople;
    private List<String> removedPeople;

    public Group(Builder builder) {
        this.groupNumber = builder.groupNumber;
        this.spreadSheetId = builder.spreadSheetId;
        this.leaderName = builder.leaderName;
        this.rowWithMonths = builder.rowWithMonths;
        this.rowWithDates = builder.rowWithDates;
        this.rowWithDays = builder.rowWithDays;
        this.groupDay = builder.groupDay;
        this.dataFirstRow = builder.dataFirstRow;
        this.peopleColumn = builder.peopleColumn;
        this.addedPeople = builder.addedPeople;
        this.removedPeople= builder.removedPeople;
    }

    public static Builder builder() { return new Builder(); }

    @Override
    public int compareTo(Group o) {
        return this.leaderName.compareTo(o.leaderName);
    }

    public static class Builder {

        private int groupNumber;
        private String spreadSheetId;
        private String leaderName;
        private int rowWithMonths;
        private int rowWithDates;
        private int rowWithDays;
        private DayOfWeek groupDay;
        private int dataFirstRow;
        private String peopleColumn;
        private List<String> addedPeople;
        private List<String> removedPeople;

        public Builder groupNumber(int groupNumber) {
            this.groupNumber = groupNumber;
            return this;
        }

        public Builder spreadSheetId(String spreadSheetId) {
            this.spreadSheetId = spreadSheetId;
            return this;
        }

        public Builder leaderName(String leaderName) {
            this.leaderName = leaderName;
            return this;
        }

        public Builder monthsRow(int monthsRow) {
            this.rowWithMonths = monthsRow;
            return this;
        }

        public Builder rowWithDates(int rowWithDates) {
            this.rowWithDates = rowWithDates;
            return this;
        }

        public Builder rowWithDays(int rowWithDays) {
            this.rowWithDays = rowWithDays;
            return this;
        }

        public Builder groupDay(DayOfWeek groupDay) {
            this.groupDay = groupDay;
            return this;
        }

        public Builder dataStartRow(int dataStartRow) {
            this.dataFirstRow = dataStartRow;
            return this;
        }

        public Builder peopleColumn(String peopleColumn) {
            this.peopleColumn = peopleColumn;
            return this;
        }

        public Builder addedPeople(List<String> addedPeople) {
            this.addedPeople = addedPeople;
            return this;
        }

        public Builder removedPeople(List<String> removedPeople) {
            this.removedPeople = removedPeople;
            return this;
        }

        public Group build() {
            return new Group(this);
        }
    }

    public String getSpreadSheetId() {
        return spreadSheetId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public int getRowWithMonths() {
        return rowWithMonths;
    }

    public DayOfWeek getGroupDay() {
        return groupDay;
    }

    public int getDataFirstRow() {
        return dataFirstRow;
    }

    public int getRowWithDates() {
        return rowWithDates;
    }

    public int getRowWithDays() {
        return rowWithDays;
    }

    public String getPeopleColumn() {
        return peopleColumn;
    }

    public List<String> getAddedPeople() {
        return addedPeople;
    }

    public List<String> getRemovedPeople() {
        return removedPeople;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("groupNumber", groupNumber)
                .add("spreadSheetId", spreadSheetId)
                .add("leaderName", leaderName)
                .add("rowWithMonths", rowWithMonths)
                .add("rowWithDates", rowWithDates)
                .add("rowWithDays", rowWithDays)
                .add("groupDay", groupDay)
                .add("dataFirstRow", dataFirstRow)
                .add("peopleColumn", peopleColumn)
                .add("addedPeople", addedPeople)
                .add("removedPeople", removedPeople)
                .toString();
    }
}
