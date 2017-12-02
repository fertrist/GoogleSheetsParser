package app.data;

import app.entities.Group;
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
    private ColumnToDateMapper columnToDateMapper;
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
        this.columnToDateMapper = new ColumnToDateMapper(this);
    }

    public void initReportLimit() {
        this.reportRange = columnToDateMapper.getReportLimit();
    }

    public ReportRange getReportRange() {
        return reportRange;
    }

    public void setReportRange(ReportRange reportRange) {
        this.reportRange = reportRange;
    }

    public ColumnToDateMapper getColumnToDateMapper() {
        return columnToDateMapper;
    }
}
