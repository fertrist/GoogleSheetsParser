package app.utils;

import app.entities.MonthData;
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

    public Map<Integer, LocalDate> initColumnToDateMapFromTableData(List<MonthData> monthDatas,
                                                                    Pair<Integer, Integer> reportLimits,
                                                                    List<CellData> datesCells) {
        columnToDateMap = new HashMap<>();
        int currentYear = LocalDate.now().getYear();

        for (MonthData month : monthDatas) {

            if (month.getStart() > reportLimits.getValue()
                    || month.getEnd() < reportLimits.getKey()) continue;

            for (int i = month.getStart(); i <= month.getEnd(); i++) {
                if (i < reportLimits.getKey() || i > reportLimits.getValue()) continue;

                CellData cell = datesCells.get(i - 1);
                if (cell.size() == 0 || cell.getEffectiveValue() == null) {
                    cell = datesCells.get(i - 1 - 1);
                }
                int dayOfMonth = cell.getEffectiveValue().getNumberValue().intValue();
                columnToDateMap.put(i - reportLimits.getKey(), LocalDate.of(currentYear, month.getMonthNumber(), dayOfMonth));
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
}
