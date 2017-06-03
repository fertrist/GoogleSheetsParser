package app.entities;

import com.google.common.base.Objects;

import java.util.List;

public class Group implements Comparable<Group> {

    private int groupNumber;
    private String spreadSheetId;
    private String leaderName;
    private String rowWithMonths;
    private String groupDay;
    private String colorsRow;
    private String dataFirstRow;
    private String dataLastRow;
    private String peopleColumn;
    private List<String> addedPeople;
    private List<String> removedPeople;

    public Group(Builder builder) {
        this.groupNumber = builder.groupNumber;
        this.spreadSheetId = builder.spreadSheetId;
        this.leaderName = builder.leaderName;
        this.rowWithMonths = builder.rowWithMonths;
        this.groupDay = builder.groupDay;
        this.colorsRow = builder.colorsRow;
        this.dataFirstRow = builder.dataFirstRow;
        this.dataLastRow = builder.dataLastRow;
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
        private String rowWithMonths;
        private String groupDay;
        private String colorsRow;
        private String dataFirstRow;
        private String dataLastRow;
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

        public Builder monthsRow(String monthsRow) {
            this.rowWithMonths = monthsRow;
            return this;
        }

        public Builder groupDay(String groupDay) {
            this.groupDay = groupDay;
            return this;
        }

        public Builder markingRow(String markingRow) {
            this.colorsRow = markingRow;
            return this;
        }

        public Builder dataStartRow(String dataStartRow) {
            this.dataFirstRow = dataStartRow;
            return this;
        }

        public Builder dataEndRow(String dataEndRow) {
            this.dataLastRow = dataEndRow;
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

    public int getGroupNumber() {
        return groupNumber;
    }

    public String getSpreadSheetId() {
        return spreadSheetId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public String getRowWithMonths() {
        return rowWithMonths;
    }

    public String getGroupDay() {
        return groupDay;
    }

    public String getColorsRow() {
        return colorsRow;
    }

    public String getDataFirstRow() {
        return dataFirstRow;
    }

    public String getDataLastRow() {
        return dataLastRow;
    }

    public String getPeopleColumn() {
        return peopleColumn;
    }

    public void setGroupNumber(int groupNumber) {
        this.groupNumber = groupNumber;
    }

    public void setSpreadSheetId(String spreadSheetId) {
        this.spreadSheetId = spreadSheetId;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public void setRowWithMonths(String rowWithMonths) {
        this.rowWithMonths = rowWithMonths;
    }

    public void setGroupDay(String groupDay) {
        this.groupDay = groupDay;
    }

    public void setColorsRow(String colorsRow) {
        this.colorsRow = colorsRow;
    }

    public void setDataFirstRow(String dataFirstRow) {
        this.dataFirstRow = dataFirstRow;
    }

    public void setDataLastRow(String dataLastRow) {
        this.dataLastRow = dataLastRow;
    }

    public void setPeopleColumn(String peopleColumn) {
        this.peopleColumn = peopleColumn;
    }

    public List<String> getAddedPeople() {
        return addedPeople;
    }

    public void setAddedPeople(List<String> addedPeople) {
        this.addedPeople = addedPeople;
    }

    public List<String> getRemovedPeople() {
        return removedPeople;
    }

    public void setRemovedPeople(List<String> removedPeople) {
        this.removedPeople = removedPeople;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("groupNumber", groupNumber)
                .add("spreadSheetId", spreadSheetId)
                .add("leaderName", leaderName)
                .add("rowWithMonths", rowWithMonths)
                .add("groupDay", groupDay)
                .add("colorsRow", colorsRow)
                .add("dataFirstRow", dataFirstRow)
                .add("dataLastRow", dataLastRow)
                .add("peopleColumn", peopleColumn)
                .toString();
    }
}
