package app;

import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RowData;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class GroupTableData
{
    private RowData monthsRow;

    private RowData datesRow;

    private List<GridRange> merges;

    private List<RowData> data;

    private Map<Integer, LocalDate> columnToDateMap;

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
}
