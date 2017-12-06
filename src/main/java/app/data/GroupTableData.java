package app.data;

import app.entities.Group;
import app.entities.Person;
import app.report.ReportRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RowData;

import java.util.List;

public class GroupTableData
{
    private Group group;
    private List<GridRange> merges;
    private RowData monthsRow;
    private RowData datesRow;
    private List<RowData> data;
    private ColumnDateMapper columnDateMapper;
    private ColorActionMapper colorActionMapper;
    private List<Person> people;
    private ReportRange reportRange;

    public RowData getMonthsRow() {
        return monthsRow;
    }

    public void setMonthsRow(RowData monthsRow) {
        this.monthsRow = monthsRow;
    }

    public RowData getDatesRow() {
        return datesRow;
    }

    public void setDatesRow(RowData datesRow) {
        this.datesRow = datesRow;
    }

    public List<GridRange> getMergedCells() {
        return merges;
    }

    public void setMerges(List<GridRange> merges) {
        this.merges = merges;
    }

    public List<RowData> getData() {
        return data;
    }

    public void setData(List<RowData> data) {
        this.data = data;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void initColumnToDateMapper() {
        this.columnDateMapper = new ColumnDateMapper(this);
    }

    public void initReportLimit() {
        this.reportRange = columnDateMapper.getReportLimit();
    }

    public ReportRange getReportRange() {
        return reportRange;
    }

    public void setReportRange(ReportRange reportRange) {
        this.reportRange = reportRange;
    }

    public ColumnDateMapper getColumnDateMapper() {
        return columnDateMapper;
    }

    public List<GridRange> getMerges() {
        return merges;
    }

    public void setColumnDateMapper(ColumnDateMapper columnDateMapper) {
        this.columnDateMapper = columnDateMapper;
    }

    public ColorActionMapper getColorActionMapper() {
        return colorActionMapper;
    }

    public void setColorActionMapper(ColorActionMapper colorActionMapper) {
        this.colorActionMapper = colorActionMapper;
    }

    public List<Person> getPeople() {
        return people;
    }

    public void setPeople(List<Person> people) {
        this.people = people;
    }
}
