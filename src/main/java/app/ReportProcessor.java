package app;

import app.data.CustomSheetApi;
import app.entities.*;
import app.enums.Category;
import app.enums.Actions;
import app.utils.Configuration;
import app.utils.SheetsApp;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Color;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import sun.java2d.xr.MutableInteger;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static app.enums.Actions.GROUP;
import static app.utils.Configuration.*;
import static app.utils.ParseHelper.*;
import static app.utils.ParseHelper.parsePeople;
import static app.utils.ReportUtil.*;
import static app.utils.Configuration.getProperty;
import static app.utils.ReportHelper.*;
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

        String spreadsheetId = group.getSpreadSheetId();

        Sheet sheet = sheetApi.getSheet(spreadsheetId, group.getRowWithMonths(), group.getDataFirstRow() - 1);

        List<GridRange> monthMerges = sheet.getMerges();

        RowData monthsRow = sheet.getData().get(0).getRowData().get(0);
        List<CellData> monthCells = monthsRow.getValues();

        RowData datesRow = sheet.getData().get(0).getRowData().get(2);
        List<CellData> dateCells = datesRow.getValues();

        Map<String, Pair<Integer, Integer>> monthLimits = getMonthLimits(monthMerges, monthCells);

        List<String> coveredMonths = getCoveredMonths(monthLimits);

        monthLimits = new LinkedHashMap<>(monthLimits.entrySet().stream().filter(e -> coveredMonths.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        Pair<Integer, Integer> exactColumns = getExactColumnsForReportData(dateCells, monthLimits);

        Map<Integer, LocalDate> columnToDateMap = getColumnToDateMap(monthLimits, dateCells);

        System.out.printf("roughColumnsRange : [%s : %s] %n",
                columnToLetter(exactColumns.getKey()), columnToLetter(exactColumns.getValue()));


        // parse people and colors
        String numberColumn = String.valueOf((char) (group.getPeopleColumn().toCharArray()[0] - 1));
        List<RowData> peopleColorRows = sheetApi
                .getRowsData(spreadsheetId, group.getDataFirstRow(), MAX_ROWS, numberColumn, group.getPeopleColumn());

        List<Person> people = parsePeople(peopleColorRows, group);

        int dataOffset = group.getDataFirstRow();

        Pair<Integer, Integer> rowsLimit = getLastDataAndColorsRow(peopleColorRows, dataOffset);
        int lastDataRow = rowsLimit.getKey();
        int colorsRow = rowsLimit.getValue();

        Map<Actions, Color> colors = parseColors(peopleColorRows, colorsRow - dataOffset);


        // get data and parse it
        List<RowData> data = sheetApi.getRowsData(spreadsheetId,
                group.getRowWithMonths(), lastDataRow, exactColumns.getKey(), exactColumns.getValue());

        List<Item> items = getItems(people, colors, exactColumns, data, columnToDateMap);

        List<Week> weeks = getWeeks(data, exactColumns, columnToDateMap, group, colors);
        // TODO test new getWeeks method

        weeks = fillWeeks(items, people, weeks);

        handleAddedRemovedToList(weeks, group, people);

        return weeks;

    }

    private static Map<String, Pair<Integer, Integer>> getMonthLimits(List<GridRange> merges, List<CellData> monthCells) {
        merges.sort((Comparator.comparing(GridRange::getStartColumnIndex)));

        //sort and map merges to start end indexes
        Map<String, Pair<Integer, Integer>> monthsMap = new HashMap<>();
        for (GridRange merge : merges) {

            int mergeStartIndex = merge.getStartColumnIndex();

            if (monthCells.get(mergeStartIndex).getEffectiveValue() == null) continue;

            String monthName = monthCells.get(mergeStartIndex).getEffectiveValue().getStringValue();
            monthName = getMonthFromString(monthName);

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

        if (startColumn == 0)
            startColumn = limits.get(0).getKey();

        if (endColumn == 0)
            endColumn = limits.get(limits.size()-1).getValue();

        return new Pair<>(startColumn, endColumn);
    }

    private static int getColumnForReportStartDay(List<CellData> dateCells, int start, int end) {
        int dateCellIndex = start;
        while (dateCellIndex < end) {
            int day = dateCells.get(dateCellIndex).getEffectiveValue().getNumberValue().intValue();
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
            if (containsIgnoreCase(group.getRemovedPeople(), person.getName())) {
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

    private static List<Item> getItems(
            List<Person> people, Map<Actions, Color> colors, Pair<Integer, Integer> startEndColumns,
            List<RowData> dataRows, Map<Integer, LocalDate> columnToDateMap)
    {
        List<Item> items = new ArrayList<>();
        for (Person person : people)
        {
            List<CellData> personCells = dataRows.get(person.getIndex()).getValues();
            if (personCells == null || personCells.isEmpty()) continue;
            int diff = startEndColumns.getValue() - startEndColumns.getKey();
            for (int i = 0; i < Math.min(personCells.size(), diff); i++)
            {
                CellData cell = personCells.get(i);
                if (cell.getEffectiveFormat() == null
                        || cell.getEffectiveFormat().getBackgroundColor() == null) {
                    continue;
                }
                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
                Actions action = getActionByColor(bgColor, colors);
                if (action != null) {
                    items.add(new Item(person.clone(), action, columnToDateMap.get(i + startEndColumns.getKey())));
                }
            }
        }
        return items;
    }

    private static List<Week> getWeeks(List<RowData> rows, Group group, Map<Integer, LocalDate> columnToDateMap,
                                       Color groupColor) {
        LocalDate reportStart = LocalDate.parse(getReportStartDate());
        LocalDate reportEnd = LocalDate.parse(getReportEndDate());

        List<Week> weeks = getWeeksFromDates(reportStart, reportEnd);

        for (Week week : weeks) {
            Integer groupDay = Integer.valueOf(group.getGroupDay());
            LocalDate groupDate = week.getStart().plusDays(groupDay - 1);

            setWeekComments(week, rows, columnToDateMap, groupDate, groupColor);
        }

        return weeks;
    }

    private static void setWeekComments(Week week, List<RowData> rows, Map<Integer, LocalDate> columnToDateMap,
                                         LocalDate groupDate, Color groupColor) {

        RowData daysRow = rows.get(1);
        List<CellData> daysCells = daysRow.getValues();

        RowData datesRow = rows.get(2);
        List<CellData> datesCells = datesRow.getValues();

        Integer[] groupDayColumns = getGroupDayColumns(groupDate, columnToDateMap);
        int groupColumn  = getGroupDayColumn(groupDayColumns, rows, groupColor);

        CellData dayCell = daysCells.get(groupColumn);
        CellData dateCell = datesCells.get(groupColumn);

        String groupNote = dayCell.getNote();

        setGroupComments(week, daysCells.get(groupColumn), datesCells.get(groupColumn));
    }

    private static int getGroupDayColumn(Integer [] groupDayColumns, List<RowData> rows, Color groupColor) {
        for (RowData row : rows) {

            if (isRowEmpty(row)) continue;

            for (int column : groupDayColumns) {
                CellData cell = row.getValues().get(column);
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

    private static List<Week> getWeeks(List<RowData> dataRows, Pair<Integer, Integer> startEndColumn,
                                       Map<Integer, LocalDate> columnToDateMap, Group group, Map<Actions, Color> colors) {
        int dataFirstRow = group.getDataFirstRow();
        dataFirstRow--; // offset because of indexing
        String groupWeekDay = getWeekDay(Integer.valueOf(group.getGroupDay()));

        RowData daysRow = dataRows.get(1);
        List<CellData> daysCells = daysRow.getValues();

        RowData datesRow = dataRows.get(2);
        List<CellData> datesCells = datesRow.getValues();

        List<Week> weeks = new ArrayList<>();

        int diff = startEndColumn.getValue() - startEndColumn.getKey();

        for (int weekIndex = 0; weekIndex < diff; weekIndex++)
        {
            int dayIndex = weekIndex - 1;
            int groupDayIndex = 0;
            String weekDay;
            do {
                dayIndex++;
                weekDay = daysCells.get(dayIndex).getEffectiveValue().getStringValue();
                if (weekDay.equalsIgnoreCase(groupWeekDay))
                {
                    for (int i = dataFirstRow; i < dataRows.size(); i++) {
                        RowData personRow = dataRows.get(i);
                        if (personRow.size() == 0 ) continue;
                        CellData cell = personRow.getValues().get(dayIndex);
                        int index = defineIsAGroupIndex(weekDay, groupWeekDay, cell, colors.get(GROUP), dayIndex);
                        groupDayIndex = index == 0 ? groupDayIndex : index;
                    }
                }
            } while (!weekDay.equalsIgnoreCase("вс") && dayIndex < (datesCells.size() - 1));

            Week week = new Week();
            week.setStart(columnToDateMap.get(startEndColumn.getKey() + weekIndex));

            setGroupComments(week, daysCells.get(groupDayIndex), datesCells.get(groupDayIndex));

            weekIndex += (dayIndex - weekIndex);

            week.setEnd(columnToDateMap.get(startEndColumn.getKey() + weekIndex));

            weeks.add(week);
        }
        return weeks;
    }

    private static int defineIsAGroupIndex(String weekDay, String groupWeekDay, CellData cell, Color groupColor, int index) {

        if (weekDay.equalsIgnoreCase(groupWeekDay)) {
            if (cell.getEffectiveFormat() == null
                    || cell.getEffectiveFormat().getBackgroundColor() == null){
                return 0;
            }
            Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
            boolean personWasPresentInGroup = areColorsEqual(bgColor, groupColor);
            if (personWasPresentInGroup)
            {
                return index;
            }
        }
        return 0;
    }

    private static void setGroupComments(Week week, CellData dayCell, CellData dateCell) {

        String groupNote = dayCell.getNote();
        groupNote = groupNote != null ? groupNote : dateCell.getNote();

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

        // TODO add report main header (report summary)
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

        // TODO add report main footer (current summary)
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

        // TODO make header frozen, create borders
        UpdateBordersRequest updateBordersRequest = new UpdateBordersRequest();
        GridRange borderRange = new GridRange().setStartColumnIndex(0).setEndColumnIndex(getReportColumns().length + 1)
                .setStartRowIndex(1).setEndRowIndex(rowPointer.getValue() + 1).setSheetId(sheetGid);
        updateBordersRequest.setRange(borderRange);
        Border border = new Border().setColor(getColor(0, 0, 0)).setStyle("SOLID");
        updateBordersRequest.setTop(border).setLeft(border).setBottom(border).setRight(border)
                .setInnerHorizontal(border).setInnerVertical(border);

        UpdateSheetPropertiesRequest freezeHeaderUpdateTitleRequest = new UpdateSheetPropertiesRequest().setFields("*");
        GridProperties gridProperties = new GridProperties().setFrozenRowCount(2).setRowCount(100)
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
        // TODO print region header
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