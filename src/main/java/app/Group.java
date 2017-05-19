package app;

import com.google.common.base.Objects;

public class Group {

    private int groupNumber;
    private String spreadSheetId;
    private String leaderName;
    private String rowWithMonths;
    private String groupDay;
    private String colorsRow;
    private String dataFirstRow;
    private String dataLastRow;
    private String peopleColumn;

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
    }

    public static Builder builder() { return new Builder(); }

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
