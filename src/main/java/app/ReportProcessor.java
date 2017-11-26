package app;

import app.data.CustomSheetApi;
import app.entities.*;
import app.enums.Actions;
import app.enums.Category;
import app.utils.Configuration;
import app.utils.MutableInteger;
import app.utils.SheetsApp;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.common.collect.Lists;
import javafx.util.Pair;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static app.enums.Actions.GROUP;
import static app.utils.Configuration.*;
import static app.utils.ParseHelper.*;
import static app.utils.ReportHelper.*;
import static app.utils.ReportUtil.*;
import static java.util.Collections.singletonList;

/**
 * Pass CONFIGURATION_FILE as vm option for example:
 * -DCONFIGURATION_FILE=/home/myUser/.../resources/report.configuration.properties
 */
public class ReportProcessor extends SheetsApp {

    private static final int MAX_ROWS = 120;

    private static CustomSheetApi sheetApi;

    public static void main(String[] args) throws IOException {
        Sheets sheetsService = getSheetsService();
        sheetApi = new CustomSheetApi(sheetsService);
        process(sheetsService);
    }

    private static void process(Sheets service) throws IOException {
        String[] regionNumbers = getProperty(REGIONS).split(",");
        List<Region> regions = new ArrayList<>();
        for (String regionNumber : regionNumbers){
            regions.add(processRegion(regionNumber));
        }
        report(service, regions);
    }

    private static Region processRegion(String regionNo) throws IOException {
        String regionLeader = getRegionProperty(LEADER, regionNo);
        System.out.println("Processing " + regionLeader + "'s region.");

        // get groups data from properties
        String [] groupNumbers = getRegionProperty(GROUPS, regionNo).split(",");
        List<Group> groups = Arrays.stream(groupNumbers)
                .map(Integer::valueOf)
                .map(Configuration::buildGroup)
                .collect(Collectors.toList());

        // loop through groups, collect and print data
        Region region = new Region(regionLeader);
        for (Group group : groups) {
            List<Week> weeks = processGroup(group);
            region.getGroups().put(group, weeks);
        }
        return region;
    }

    private static List<Week> processGroup(Group group) throws IOException {
        // get start end columns for report
        System.out.println("Processing " + group.getLeaderName() + "'s group.");

        String spreadsheetId = group.getSpreadSheetId();

        Sheet sheet = sheetApi.getSheet(spreadsheetId, group.getRowWithMonths(), group.getDataFirstRow() - 1);

        List<GridRange> monthMerges = sheet.getMerges();

        RowData monthsRow = sheet.getData().get(0).getRowData().get(toIndex(group.getRowWithMonths()));
        List<CellData> monthCells = monthsRow.getValues();

        RowData datesRow = sheet.getData().get(0).getRowData().get(toIndex(group.getRowWithDates()));
        List<CellData> dateCells = datesRow.getValues();

        Map<String, Pair<Integer, Integer>> monthLimits = getMonthLimits(monthMerges, monthCells);

        List<String> coveredMonths = getCoveredMonths(monthLimits);

        monthLimits = new LinkedHashMap<>(monthLimits.entrySet().stream().filter(e -> coveredMonths.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        Pair<Integer, Integer> reportColumns = getExactColumnsForReportData(dateCells, monthLimits);

        System.out.printf("Columns Range : [%s : %s] %n",
                columnToLetter(reportColumns.getKey()), columnToLetter(reportColumns.getValue()));

        Map<Integer, LocalDate> columnToDateMap = getColumnToDateMap(monthLimits, reportColumns, dateCells);

        // parse people and colors
        String numberColumn = String.valueOf((char) (group.getPeopleColumn().toCharArray()[0] - 1));

        List<RowData> peopleColorRows = sheetApi
                .getRowsData(spreadsheetId, group.getDataFirstRow(), MAX_ROWS, numberColumn, group.getPeopleColumn());

        List<Person> people = parsePeople(peopleColorRows, group);

        int dataOffset = group.getDataFirstRow();

        Pair<Integer, Integer> dataColorRows = getLastDataAndColorsRow(peopleColorRows, dataOffset);
        int lastDataRow = dataColorRows.getKey();
        int colorsRow = dataColorRows.getValue();

        Map<Actions, Color> colors = parseColors(peopleColorRows, colorsRow - dataOffset);


        // get data and parse it
        List<RowData> data = sheetApi.getRowsData(spreadsheetId,
                group.getRowWithMonths(), lastDataRow, reportColumns.getKey(), reportColumns.getValue());

        List<Item> items = getItems(people, colors, reportColumns, data, columnToDateMap);

        List<Week> weeks = getWeeks(data, group, columnToDateMap, colors.get(GROUP));

        weeks = fillWeeks(items, people, weeks);

        handleAddedRemovedToList(weeks, group, people);

        return weeks;
    }

    private static int toIndex(int value) {
        return value - 1;
    }

    private static Map<String, Pair<Integer, Integer>> getMonthLimits(List<GridRange> merges, List<CellData> monthCells) {
        merges.sort((Comparator.comparing(GridRange::getStartColumnIndex)));

        //sort and map merges to start end indexes
        Map<String, Pair<Integer, Integer>> monthsMap = new HashMap<>();
        for (GridRange merge : merges) {

            int mergeStartIndex = merge.getStartColumnIndex();

            if (monthCells.get(mergeStartIndex).getEffectiveValue() == null) continue;

            String monthName = monthCells.get(mergeStartIndex).getEffectiveValue().getStringValue();
            monthName = getMonthNameFromString(monthName);

            monthsMap.put(monthName, new Pair<>(merge.getStartColumnIndex()+1, merge.getEndColumnIndex()));
        }
        return monthsMap;
    }

    /**
     * Get raw, approximate, rough range of columns to work with (to avoid parsing old columns)
     */
    private static Pair<Integer, Integer> getExactColumnsForReportData(List<CellData> dateCells,
                                                                       Map<String, Pair<Integer, Integer>> monthLimits) {
        // define start/end
        int startColumn = 0;
        int endColumn = 0;

        for (Map.Entry<String, Pair<Integer, Integer>> monthLimit : monthLimits.entrySet()) {

            String month = monthLimit.getKey();
            Pair<Integer, Integer> limit = monthLimit.getValue();

            if (month.equals(getReportStartMonth())) {
                startColumn = getColumnForReportStartDay(dateCells, limit.getKey(), limit.getValue());
            }
            if (month.equals(getReportEndMonth())) {
                endColumn = getColumnForReportEndDay(dateCells, limit.getKey(), limit.getValue());
            }
        }
        // if start or end month is missed, use what we have

        List<Pair<Integer, Integer>> limits = new ArrayList<>(monthLimits.values());
        limits.sort(Comparator.comparing(Pair::getKey));

        if (startColumn == 0)
            startColumn = limits.get(0).getKey();

        if (endColumn == 0)
            endColumn = limits.get(limits.size()-1).getValue();

        return new Pair<>(startColumn, endColumn);
    }

    private static int getColumnForReportStartDay(List<CellData> dateCells, int start, int end) {
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

    private static int getColumnForReportEndDay(List<CellData> dateCells, int start, int end) {
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

    private static void handleAddedRemovedToList(List<Week> weeks, Group group, List<Person> people) {

        Week week = weeks.get(weeks.size() - 1);

        for (Person person : people)
        {
            if (containsIgnoreCase(group.getAddedPeople(), person.getName())) {
                Person updated = person.clone();
                updated.setCategory(Category.WHITE);
                week.getWhiteList().add(updated);
            }
            else if (containsIgnoreCase(group.getRemovedPeople(), person.getName())) {
                Person updated = person.clone();
                week.getWhiteList().remove(updated);
                updated.setCategory(Category.NEW);
            }
        }
        for (Item item : week.getItems()) {
            if (containsIgnoreCase(group.getAddedPeople(), item.getPerson().getName())) {
                item.getPerson().setCategory(Category.WHITE);
            }
            if (containsIgnoreCase(group.getRemovedPeople(), item.getPerson().getName())) {
                item.getPerson().setCategory(Category.NEW);
            }
        }
    }

    private static List<Week> fillWeeks(List<Item> items, List<Person> people, List<Week> weeks) {

        weeks.sort(Comparator.comparing(Week::getStart));
        List<Person> whiteList = people.stream().filter(p -> p.getCategory() == Category.WHITE)
                .map(Person::clone).collect(Collectors.toList());
        weeks.forEach(week -> week.getWhiteList().addAll(whiteList));
        weeks.forEach(
                w -> w.getItems().addAll(items.stream()
                    .filter(i -> withinStartEnd(i.getDate(), w.getStart(), w.getEnd())).collect(Collectors.toList())
                ));
        return weeks;
    }

    private static boolean withinStartEnd(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isAfter(start) || date.isEqual(start)) && (date.isBefore(end) || date.isEqual(end));
    }

    private static List<Item> getItems(List<Person> people, Map<Actions, Color> colors,
                                       Pair<Integer, Integer> startEndColumns,
            List<RowData> dataRows, Map<Integer, LocalDate> columnToDateMap)
    {
        List<Item> items = new ArrayList<>();
        int diff = startEndColumns.getValue() - startEndColumns.getKey();
        for (Person person : people)
        {
            // case where row is empty for the person thus not fetched
            if (dataRows.size() < person.getIndex()+1)
            {
                continue;
            }
            RowData row = dataRows.get(person.getIndex());

            if (isRowEmpty(row)) continue;

            List<CellData> personCells = row.getValues();
            /*List<CellData> personCells = row.getValues().subList(0, diff).stream().filter(ReportUtil::hasBackground)
                    .collect(Collectors.toList());*/

            for (int i = 0; i < personCells.size(); i++)
            {
                CellData cell = personCells.get(i);

                if (!hasBackground(cell)) continue;

                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();

                Actions action = getActionByColor(bgColor, colors);

                if (action != null)
                {
                    LocalDate date = columnToDateMap.get(i);
                    items.add(new Item(person.clone(), action, date));
                }
            }
        }
        return items;
    }

    private static List<Week> getWeeks(List<RowData> rows, Group group, Map<Integer, LocalDate> columnToDateMap,
                                       Color groupColor) {
        Integer groupDay = group.getGroupDay().ordinal();
        LocalDate reportStart = LocalDate.parse(getReportStartDate());
        LocalDate reportEnd = LocalDate.parse(getReportEndDate());

        RowData daysRow = rows.get(group.getRowWithDays());
        List<CellData> daysCells = daysRow.getValues();

        RowData datesRow = rows.get(group.getRowWithDates());
        List<CellData> datesCells = datesRow.getValues();

        List<Week> weeks = getWeeksFromDates(reportStart, reportEnd);

        for (Week week : weeks) {

            LocalDate groupDate = week.getStart().plusDays(groupDay - 1);

            Integer[] groupDayColumns = getGroupDayColumns(groupDate, columnToDateMap);

            int groupColumn = getGroupDayColumn(groupDayColumns, rows, groupColor);

            if (groupColumn == -1) continue;

            CellData dayCell = daysCells.get(groupColumn);
            CellData dateCell = datesCells.get(groupColumn);

            String groupNote = dayCell.getNote();
            groupNote = groupNote != null ? groupNote : dateCell.getNote();

            setGroupComments(week, groupNote);
        }
        return weeks;
    }

    private static int getGroupDayColumn(Integer [] groupDayColumns, List<RowData> rows, Color groupColor) {
        for (RowData row : rows) {

            if (isRowEmpty(row)) continue;

            for (int column : groupDayColumns) {
                if (column > row.getValues().size()) continue;

                CellData cell = row.getValues().get(column);

                if (!hasBackground(cell)) continue;

                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
                if (areColorsEqual(bgColor, groupColor)) {
                    return column;
                }
            }
        }
        return -1;
    }

    private static Integer[] getGroupDayColumns(LocalDate groupDate, Map<Integer, LocalDate> columnToDateMap) {

        List<Integer> columns = new ArrayList<>();
        for (Map.Entry<Integer, LocalDate> entry : columnToDateMap.entrySet()) {
            if (entry.getValue().isEqual(groupDate)) {
                columns.add(entry.getKey());
            }
        }
        return columns.toArray(new Integer[columns.size()]);
    }

    private static void setGroupComments(Week week, String groupNote) {

        if (groupNote != null && !groupNote.isEmpty()) {
            String firstString = groupNote.split("\\n")[0];
            if (firstString.matches("[0-9]+[%]")) {
                week.setPercents(firstString);
                String comment = groupNote
                        .substring(groupNote.indexOf(firstString), groupNote.length());
                week.setGroupComments(comment.trim());
            } else {
                week.setGroupComments(groupNote);
            }
        }
    }

    private static List<String> getCoveredMonths(Map<String, Pair<Integer, Integer>> monthsMap) {

        LocalDate reportStartDate = LocalDate.parse(getReportStartDate());
        LocalDate reportEndDate = LocalDate.parse(getReportEndDate());

        Set<String> months = new LinkedHashSet<>(); // order is important here

        for (LocalDate tmp = reportStartDate; tmp.isBefore(reportEndDate) || tmp.isEqual(reportEndDate);
             tmp = tmp.plusDays(1))
        {
            Month[] values = Month.values();
            String month = values[tmp.getMonthValue() - 1].getName();
            if (monthsMap.get(month) != null) {
                months.add(month);
            }
        }

        List<String> coveredMonths = new ArrayList<>(months);

        // validate months are recent and really are covered
        ListIterator<String> iterator = coveredMonths.listIterator();
        int previousEnd = 0;
        while (iterator.hasNext()) {
            String nextMonth = iterator.next();
            int nextStart = monthsMap.get(nextMonth).getKey();
            if (nextStart < previousEnd) {
                iterator.remove();
            } else {
                previousEnd = monthsMap.get(nextMonth).getValue();
            }
        }

        return coveredMonths;
    }

    /**
     * Reports all regions to report sheet.
     */
    private static void report(Sheets service, List<Region> regions) throws IOException {
        Integer sheetGid = getSheetGid(getProperty(REPORT_SPREADSHEET_URL));

        UpdateCellsRequest updateHeaderRequest = new UpdateCellsRequest();
        updateHeaderRequest.setFields("*");

        GridCoordinate headerGridCoordinate = new GridCoordinate();
        headerGridCoordinate.setColumnIndex(0);
        headerGridCoordinate.setSheetId(sheetGid);
        headerGridCoordinate.setRowIndex(0);
        updateHeaderRequest.setStart(headerGridCoordinate);

        // add report main header (report summary)
        List<RowData> headers = new ArrayList<>();
        headers.add(getTitleHeader());
        headers.add(getReportHeader());
        updateHeaderRequest.setRows(headers);

        MutableInteger rowPointer = new MutableInteger(0);
        rowPointer.setValue(rowPointer.getValue() + 2);

        int totalNewCount = 0;
        int totalWhiteCount = 0;

        for (Region region : regions) {
            printRegion(service, rowPointer, region);
            totalNewCount += region.getTotalNewCount();
            totalWhiteCount += region.getTotalWhiteCount();
        }

        // add report main footer (current summary)
        UpdateCellsRequest updateFooterRequest = new UpdateCellsRequest();
        updateFooterRequest.setFields("*");

        GridCoordinate footerGridCoordinate = new GridCoordinate();
        footerGridCoordinate.setColumnIndex(0);
        footerGridCoordinate.setSheetId(sheetGid);
        footerGridCoordinate.setRowIndex(rowPointer.getValue());
        updateFooterRequest.setStart(footerGridCoordinate);

        List<RowData> footers = new ArrayList<>();
        footers.add(getReportFooterRow(totalWhiteCount, totalNewCount));

        updateFooterRequest.setRows(footers);

        // make header frozen, create borders
        UpdateBordersRequest updateBordersRequest = new UpdateBordersRequest();
        GridRange borderRange = new GridRange().setStartColumnIndex(0).setEndColumnIndex(getReportColumns().length + 1)
                .setStartRowIndex(1).setEndRowIndex(rowPointer.getValue() + 1).setSheetId(sheetGid);
        updateBordersRequest.setRange(borderRange);
        Border border = new Border().setColor(getColor(0, 0, 0)).setStyle("SOLID");
        updateBordersRequest.setTop(border).setLeft(border).setBottom(border).setRight(border)
                .setInnerHorizontal(border).setInnerVertical(border);

        UpdateSheetPropertiesRequest freezeHeaderUpdateTitleRequest = new UpdateSheetPropertiesRequest().setFields("*");
        GridProperties gridProperties = new GridProperties().setFrozenRowCount(2).setRowCount(MAX_ROWS + 30)
                .setColumnCount(getReportColumns().length);

        SheetProperties sheetProperties = new SheetProperties().setGridProperties(gridProperties)
                .setTitle(getProperty(REPORT_TITLE)).setSheetId(sheetGid);
        freezeHeaderUpdateTitleRequest.setProperties(sheetProperties);

        // align all cells
        RepeatCellRequest alignAllCellsRequest = new RepeatCellRequest().setFields("userEnteredFormat.horizontal_alignment")
                .setRange(borderRange.setStartRowIndex(0))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat().setHorizontalAlignment("CENTER")));

        MergeCellsRequest mergeTitleNameRequest = new MergeCellsRequest().setMergeType("MERGE_ALL")
                .setRange(new GridRange().setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(0).setEndColumnIndex(2)
                        .setSheetId(sheetGid));
        // resize columns
        /*UpdateDimensionPropertiesRequest resizeRequest = new UpdateDimensionPropertiesRequest().setFields("")
                .setRange(new DimensionRange().setDimension("COLUMNS").setSheetId(sheetGid)
                        .setStartIndex(0).setEndIndex(getReportColumns().length + 1))
                .setProperties(new DimensionProperties().setPixelSize())*/

        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setUpdateCells(updateHeaderRequest));
        requests.add(new Request().setUpdateCells(updateFooterRequest));
        requests.add(new Request().setUpdateBorders(updateBordersRequest));
        requests.add(new Request().setUpdateSheetProperties(freezeHeaderUpdateTitleRequest));
        requests.add(new Request().setRepeatCell(alignAllCellsRequest));
        requests.add(new Request().setMergeCells(mergeTitleNameRequest));
        //requests.add(new Request().setUpdateDimensionProperties(resizeRequest));

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();
    }

    /**
     * Prints single region to the report sheet.
     * @param rowPointer which counts offset of rows, to print next items correctly
     */
    private static void printRegion(Sheets service, MutableInteger rowPointer, Region region) throws IOException {
        // print region header
        List<Request> requests = new ArrayList<>();

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest().setFields("*");

        GridCoordinate gridCoordinate = new GridCoordinate().setColumnIndex(0)
                .setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL))).setRowIndex(rowPointer.getValue());

        updateCellsRequest.setStart(gridCoordinate);

        updateCellsRequest.setRows(singletonList(new RowData().setValues(
                        Lists.asList(getCellWithBgColor(region.getLeader(), GREY), Collections.nCopies(12, getCellWithBgColor(GREY)).toArray(new CellData[12])))
        ));

        MergeCellsRequest mergeCellsRequest = new MergeCellsRequest().setMergeType("MERGE_ALL");
        GridRange range = new GridRange().setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL)))
                .setStartRowIndex(rowPointer.getValue()).setEndRowIndex(rowPointer.getValue() + 1)
                .setStartColumnIndex(0).setEndColumnIndex(getReportColumns().length);
        mergeCellsRequest.setRange(range);

        rowPointer.setValue(rowPointer.getValue() + 1);

        requests.add(new Request().setUpdateCells(updateCellsRequest));
        requests.add(new Request().setMergeCells(mergeCellsRequest));

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();

        for (Map.Entry<Group, List<Week>> groupEntry : region.getGroups().entrySet()) {
            List<Week> groupWeeks = groupEntry.getValue();
            printWeeks(service, groupEntry.getKey(), groupWeeks, rowPointer.getValue());
            rowPointer.setValue(rowPointer.getValue() + groupWeeks.size() + 1);
        }
    }
}