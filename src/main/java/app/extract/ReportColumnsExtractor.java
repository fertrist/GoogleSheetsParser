package app.extract;

import static app.conf.Configuration.getReportEndDay;
import static app.conf.Configuration.getReportEndMonth;
import static app.conf.Configuration.getReportStartDay;
import static app.conf.Configuration.getReportStartMonth;
import static app.extract.ReportUtil.columnToLetter;
import app.data.GroupTableData;
import app.report.ReportMonth;
import com.google.api.services.sheets.v4.model.CellData;
import javafx.util.Pair;

import java.util.Comparator;
import java.util.List;

public class ReportColumnsExtractor
{
    private GroupTableData groupTableData;

    public ReportColumnsExtractor(GroupTableData groupTableData)
    {
        this.groupTableData = groupTableData;
    }

    /**
     * Get raw, approximate, rough range of columns to work with (to avoid parsing old columns)
     */
    public Pair<Integer, Integer> getExactColumnsForReportData(List<ReportMonth> coveredMonths) {
        // define start/end
        int startColumn = 0;
        int endColumn = 0;

        for (ReportMonth month : coveredMonths) {

            String monthName = month.getMonth().getName();

            if (monthName.equals(getReportStartMonth())) {
                startColumn = getColumnForReportStartDay(groupTableData.getDatesRow().getValues(), month.getStart(), month.getEnd());
            }
            if (monthName.equals(getReportEndMonth())) {
                endColumn = getColumnForReportEndDay(groupTableData.getDatesRow().getValues(), month.getStart(), month.getEnd());
            }
        }
        // if start or end month is missed, use what we have

        coveredMonths.sort(Comparator.comparing(ReportMonth::getStart));

        if (startColumn == 0)
            startColumn = coveredMonths.get(0).getStart();

        if (endColumn == 0)
            endColumn = coveredMonths.get(coveredMonths.size()-1).getEnd();

        System.out.printf("Columns Range : [%s : %s] %n",
                columnToLetter(startColumn), columnToLetter(endColumn));

        return new Pair<>(startColumn, endColumn);
    }

    private int getColumnForReportStartDay(List<CellData> dateCells, int start, int end) {
        int dateCellIndex = start;
        while (dateCellIndex < end) {
            CellData cell = dateCells.get(dateCellIndex); // for case when cells are merged and value is only in first cell
            if (cell.size() == 0 || cell.getEffectiveValue() == null)
            {
                cell = dateCells.get(dateCellIndex - 1);
            }
            int day = cell.getEffectiveValue().getNumberValue().intValue();
            if (day == getReportStartDay()) {
                break;
            }
            dateCellIndex++;
        }
        return dateCellIndex + 1; // column starts from 1, while list indexing from 0
    }

    private int getColumnForReportEndDay(List<CellData> dateCells, int start, int end) {
        int dateCellIndex = end - 2; // -1 because of indexing and -1 because end is exclusive
        while (dateCellIndex >= start) {
            if (dateCells.get(dateCellIndex).size() == 0 || dateCells.get(dateCellIndex).getEffectiveValue() == null) {
                dateCellIndex--;
                continue;
            }
            int day = dateCells.get(dateCellIndex).getEffectiveValue().getNumberValue().intValue();
            if (day == getReportEndDay()) {
                break;
            }
            dateCellIndex--;
        }
        return dateCellIndex + 1; // column starts from 1, while list indexing from 0
    }
}
