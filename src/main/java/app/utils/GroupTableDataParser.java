package app.utils;

import static app.utils.Configuration.getReportEndDate;
import static app.utils.Configuration.getReportStartDate;
import static app.utils.ReportUtil.constructMonthFromName;
import app.GroupTableData;
import app.entities.MonthData;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.GridRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupTableDataParser
{
    private GroupTableData groupTableData;

    public GroupTableDataParser(GroupTableData groupTableData) {
        this.groupTableData = groupTableData;
    }

    public List<MonthData> extractMonthsRequiredForReport()
    {
        List<MonthData> monthsFromTable = extractAllMonthsFromGroupTable();

        List<MonthData> coveredMonths = getCoveredMonths(monthsFromTable);

        return coveredMonths;
    }

    private List<MonthData> extractAllMonthsFromGroupTable()
    {
        List<CellData> separateCells = groupTableData.getMonthsRow().getValues();

        List<MonthData> months = extractAllMonthsFromMonthsCells(groupTableData.getMergedCells(), separateCells);

        months.sort(Comparator.comparing(MonthData::getStart));

        return months;
    }

    private List<MonthData> extractAllMonthsFromMonthsCells(List<GridRange> mergedMonthsCells, List<CellData> separateMonthsCells)
    {
        List<MonthData> months = new ArrayList<>();

        months.addAll(mergedMonthsCells.stream()
                .map(mergedMonthCell -> extractSingleMonthFromItsTableCells(mergedMonthCell, separateMonthsCells))
                .collect(Collectors.toList()));

        return months;
    }

    private MonthData extractSingleMonthFromItsTableCells(GridRange gridRange, List<CellData> monthCells)
    {
        CellData mergeFirstCell = monthCells.get(gridRange.getStartColumnIndex());

        String monthName = getStringValueFromCell(mergeFirstCell);

        ReportUtil.Month month = constructMonthFromName(monthName);

        return combineMonthData(month, gridRange);
    }

    private static List<MonthData> getCoveredMonths(List<MonthData> monthsFromTable) {

        LocalDate reportStartDate = LocalDate.parse(getReportStartDate());
        LocalDate reportEndDate = LocalDate.parse(getReportEndDate());

        List<ReportUtil.Month> coveredMonths = getMonthsFromRange(reportStartDate, reportEndDate);

        List<MonthData> coveredMonthDatas = monthsFromTable.stream().filter(reportMonth -> coveredMonths.contains(reportMonth.getMonth())).collect(Collectors.toList());

        // clean month which have same name but are outside the range
        ListIterator<MonthData> iterator = coveredMonthDatas.listIterator();
        int previousEnd = 0;
        while (iterator.hasNext()) {
            MonthData nextMonth = iterator.next();
            int nextStart = nextMonth.getStart();
            if (nextStart < previousEnd) {
                iterator.remove();
            } else {
                previousEnd = nextMonth.getEnd();
            }
        }

        return coveredMonthDatas;
    }

    private static List<ReportUtil.Month> getMonthsFromRange(LocalDate startDate, LocalDate endDate)
    {
        Set<ReportUtil.Month> coveredMonths = new LinkedHashSet<>();

        while (lessOrEqual(startDate, endDate)) {
            ReportUtil.Month monthFromRange = convertToLocalMonth(startDate.getMonth());
            coveredMonths.add(monthFromRange);
        }

        return coveredMonths.stream().collect(Collectors.toList());
    }

    private static ReportUtil.Month convertToLocalMonth(java.time.Month month)
    {
        int monthNumber = month.ordinal();
        return ReportUtil.Month.values()[monthNumber - 1];

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

    private static MonthData combineMonthData(ReportUtil.Month month, GridRange gridRangeForMonth)
    {
        int monthStartColumn = gridRangeForMonth.getStartColumnIndex() + 1;
        int monthEndColumn = gridRangeForMonth.getEndColumnIndex();
        return new MonthData(month, monthStartColumn, monthEndColumn);
    }




}
