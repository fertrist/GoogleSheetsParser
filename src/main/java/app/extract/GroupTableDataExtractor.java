package app.extract;

import static app.conf.Configuration.getReportEndDate;
import static app.conf.Configuration.getReportStartDate;
import static app.extract.ReportUtil.constructMonthFromName;
import app.data.GroupTableData;
import app.report.ReportMonth;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.GridRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupTableDataExtractor
{
    private GroupTableData groupTableData;

    public GroupTableDataExtractor(GroupTableData groupTableData) {
        this.groupTableData = groupTableData;
    }

    public List<ReportMonth> extractMonthsRequiredForReport()
    {
        List<ReportMonth> monthsFromTable = extractAllMonthsFromGroupTable();
        return getCoveredMonths(monthsFromTable);
    }

    private List<ReportMonth> extractAllMonthsFromGroupTable()
    {
        List<CellData> separateCells = groupTableData.getMonthsRow().getValues();

        List<ReportMonth> months = extractAllMonthsFromMonthsCells(groupTableData.getMergedCells(), separateCells);

        months.sort(Comparator.comparing(ReportMonth::getStart));

        return months;
    }

    private List<ReportMonth> extractAllMonthsFromMonthsCells(List<GridRange> mergedMonthsCells, List<CellData> separateMonthsCells)
    {
        return mergedMonthsCells.stream()
                .map(mergedMonthCell -> extractSingleMonthFromItsTableCells(mergedMonthCell, separateMonthsCells))
                .collect(Collectors.toList());
    }

    private ReportMonth extractSingleMonthFromItsTableCells(GridRange gridRange, List<CellData> monthCells)
    {
        CellData mergeFirstCell = monthCells.get(gridRange.getStartColumnIndex());

        String monthName = getStringValueFromCell(mergeFirstCell);

        ReportUtil.Month month = (monthName != null && !monthName.isEmpty()) ? constructMonthFromName(monthName) : null;

        return combineMonthData(month, gridRange);
    }

    private List<ReportMonth> getCoveredMonths(List<ReportMonth> monthsFromTable) {

        LocalDate reportStartDate = LocalDate.parse(getReportStartDate());
        LocalDate reportEndDate = LocalDate.parse(getReportEndDate());

        List<ReportUtil.Month> coveredMonths = getMonthsFromRange(reportStartDate, reportEndDate);

        Map<ReportUtil.Month, ReportMonth> nonReapeatedMonths = monthsFromTable.stream()
                .filter(reportMonth -> coveredMonths.contains(reportMonth.getMonth()))
                .collect(Collectors.toMap(ReportMonth::getMonth,
                        Function.<ReportMonth>identity(), BinaryOperator.maxBy(byStartDate())));

        return nonReapeatedMonths.values().stream()
                .sorted((prev, next) -> Integer.compare(prev.getStart(), next.getStart()))
                .collect(Collectors.toList());
    }

    private Comparator<ReportMonth> byStartDate()
    {
        return (m1, m2)-> Integer.compare(m1.getStart(), m2.getStart());
    }

    private static List<ReportUtil.Month> getMonthsFromRange(LocalDate startDate, LocalDate endDate)
    {
        Set<ReportUtil.Month> coveredMonths = new LinkedHashSet<>();

        while (lessOrEqual(startDate, endDate)) {
            ReportUtil.Month monthFromRange = convertToLocalMonth(startDate.getMonth());
            coveredMonths.add(monthFromRange);
            startDate = addOneDay(startDate);
        }

        return coveredMonths.stream().collect(Collectors.toList());
    }

    private static ReportUtil.Month convertToLocalMonth(java.time.Month month)
    {
        int monthNumber = month.ordinal();
        return ReportUtil.Month.values()[monthNumber];
    }

    private static LocalDate addOneDay(LocalDate date)
    {
        return date.plusDays(1);
    }

    private static boolean lessOrEqual(LocalDate subjectDate, LocalDate dateToCompareWith)
    {
        return subjectDate.isBefore(dateToCompareWith) || subjectDate.isEqual(dateToCompareWith);
    }

    private String getStringValueFromCell(CellData cellData)
    {
       return cellData.getEffectiveValue() != null ? cellData.getEffectiveValue().getStringValue() : null;
    }

    private static ReportMonth combineMonthData(ReportUtil.Month month, GridRange gridRangeForMonth)
    {
        int monthStartColumn = gridRangeForMonth.getStartColumnIndex() + 1;
        int monthEndColumn = gridRangeForMonth.getEndColumnIndex();
        return new ReportMonth(month, monthStartColumn, monthEndColumn);
    }




}
