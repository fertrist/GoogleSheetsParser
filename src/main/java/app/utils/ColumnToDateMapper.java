package app.utils;

import app.GroupTableData;
import app.entities.ColRow;
import app.entities.MonthData;
import app.entities.ReportRange;
import com.google.api.services.sheets.v4.model.CellData;
import javafx.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnToDateMapper
{
    private Map<Integer, LocalDate> columnToDateMap;
    private List<MonthData> coveredMonths;
    private Pair<Integer, Integer> reportColumns;

    public ColumnToDateMapper(GroupTableData groupTableData) {
        this.coveredMonths = new GroupTableDataParser(groupTableData).extractMonthsRequiredForReport();
        this.reportColumns = new ReportColumnsExtractor(groupTableData).getExactColumnsForReportData(coveredMonths);
        initColumnToDateMapFromTableData(groupTableData);
    }

    public Map<Integer, LocalDate> initColumnToDateMapFromTableData(GroupTableData groupTableData) {

        List<CellData> datesCells = groupTableData.getDatesRow().getValues();

        columnToDateMap = new HashMap<>();
        int currentYear = LocalDate.now().getYear();

        for (MonthData month : coveredMonths) {

            if (month.getStart() > reportColumns.getValue()
                    || month.getEnd() < reportColumns.getKey()) continue;

            for (int i = month.getStart(); i <= month.getEnd(); i++) {
                if (i < reportColumns.getKey() || i > reportColumns.getValue()) continue;

                CellData cell = datesCells.get(i - 1);
                if (cell.size() == 0 || cell.getEffectiveValue() == null) {
                    cell = datesCells.get(i - 1 - 1);
                }
                int dayOfMonth = cell.getEffectiveValue().getNumberValue().intValue();
                columnToDateMap.put(i - reportColumns.getKey(), LocalDate.of(currentYear, month.getMonthNumber(), dayOfMonth));
            }
        }

        return columnToDateMap;
    }

    public LocalDate dateForColumn(Integer column)
    {
        return columnToDateMap.get(column);
    }

    public List<Integer> getColumnsForDate(LocalDate date)
    {
        List<Integer> columns = new ArrayList<>();
        for (Map.Entry<Integer, LocalDate> entry : columnToDateMap.entrySet()) {
            if (entry.getValue().isEqual(date)) {
                columns.add(entry.getKey());
            }
        }
        return columns;
    }

    public Pair<Integer, Integer> getReportColumns() {
        return reportColumns;
    }

    public ReportRange getReportLimit() {
        ColRow startPoint = new ColRow(reportColumns.getKey(), 0);
        ColRow endPoint = new ColRow(reportColumns.getValue(), 0);
        return new ReportRange(startPoint, endPoint);
    }
}
