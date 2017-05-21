package app;

import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Color;
import javafx.util.Pair;
import sun.java2d.xr.MutableInteger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static app.ConfigurationUtil.*;
import static app.GoogleSheetUtil.*;
import static app.ConfigurationUtil.getProperty;
import static app.ReportTableHelper.*;

/**
 * Pass CONFIGURATION_FILE as vm option for example:
 * -DCONFIGURATION_FILE=/home/myUser/.../resources/report.configuration.properties
 */
public class GroupTablesParser extends GoogleSheetsApp {

    public static void main(String[] args) throws IOException {
        Sheets service = getSheetsService();

        int regionCount = Integer.valueOf(getProperty(REGION_COUNT));
        List<RegionData> regions = new ArrayList<>();
        for (int i = 1; i <= regionCount; i++) {
            regions.add(processRegion(service, i));
        }
        report(service, regions);
    }

    private static void report(Sheets service, List<RegionData> regions) throws IOException {
        UpdateCellsRequest updateHeaderRequest = new UpdateCellsRequest();
        updateHeaderRequest.setFields("*");

        GridCoordinate headerGridCoordinate = new GridCoordinate();
        headerGridCoordinate.setColumnIndex(0);
        headerGridCoordinate.setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL)));
        headerGridCoordinate.setRowIndex(0);
        updateHeaderRequest.setStart(headerGridCoordinate);

        // TODO add report main header (report summary)
        List<RowData> headers = new ArrayList<>();
        headers.add(getTitleHeader());
        headers.add(getHeaderRow());
        updateHeaderRequest.setRows(headers);

        MutableInteger rowPointer = new MutableInteger(0);
        rowPointer.setValue(rowPointer.getValue() + 2);
        int totalNewCount = 0;
        int totalWhiteCount = 0;
        for (RegionData region : regions) {
            printRegion(service, rowPointer, region);
            totalNewCount += region.getTotalNewCount();
            totalWhiteCount += region.getTotalWhiteCount();
        }
        // TODO add report main footer (current summary)
        UpdateCellsRequest updateFooterRequest = new UpdateCellsRequest();
        updateFooterRequest.setFields("*");

        GridCoordinate footerGridCoordinate = new GridCoordinate();
        footerGridCoordinate.setColumnIndex(0);
        footerGridCoordinate.setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL)));
        footerGridCoordinate.setRowIndex(rowPointer.getValue());
        updateFooterRequest.setStart(footerGridCoordinate);

        List<RowData> footers = new ArrayList<>();
        footers.add(getReportFooterRow(totalWhiteCount, totalNewCount));

        updateFooterRequest.setRows(footers);

        // TODO make header frozen, create borders, merge leaders cells

        List<Request> requests = new ArrayList<>();
        Request headerRequest = new Request();
        headerRequest.setUpdateCells(updateHeaderRequest);
        requests.add(headerRequest);
        Request footerRequest = new Request();
        footerRequest.setUpdateCells(updateFooterRequest);
        requests.add(footerRequest);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();
    }

    private static void printRegion(Sheets service, MutableInteger rowPointer, RegionData region) throws IOException {
        // TODO print region header
        rowPointer.setValue(rowPointer.getValue() + 1);

        for (Map<String, List<Week>> group : region.getGroups()) {
            Map.Entry<String, List<Week>> groupEntry = group.entrySet().iterator().next();
            String groupLeader = groupEntry.getKey();
            List<Week> groupWeeks = groupEntry.getValue();
            printWeeks(service, groupLeader, groupWeeks, rowPointer.getValue());
            rowPointer.setValue(rowPointer.getValue() + groupWeeks.size() + 1);
        }
    }

    private static RegionData processRegion(Sheets service, int regionNo) throws IOException {
        String regionLeader = getRegionProperty(LEADER, regionNo);
        System.out.println("Processing " + regionLeader + "'s region.");

        // get groups data from properties
        String [] groupNumbers = getRegionProperty(GROUPS, regionNo).split(",");
        List<Group> groups = Arrays.stream(groupNumbers)
                .map(Integer::valueOf)
                .map(ConfigurationUtil::buildGroup)
                .collect(Collectors.toList());

        //print group properties
        groups.forEach(System.out::println);

        // loop through groups, collect and print data
        RegionData regionData = new RegionData(regionLeader);
        for (Group group : groups) {
            Map<String, List<Week>> groupData = new HashMap<>();
            groupData.put(group.getLeaderName(), parseGroup(service, group));
            regionData.getGroups().add(groupData);
        }
        return regionData;
    }

    private static List<Week> parseGroup(Sheets service, Group group) throws IOException {
        String leader = group.getLeaderName();
        String spreadsheetId = group.getSpreadSheetId();
        String dataFirstRow = group.getDataFirstRow();
        String dataLastRow = group.getDataLastRow();
        String colorsRow = group.getColorsRow();
        String groupDay = group.getGroupDay();
        String rowWithMonths = group.getRowWithMonths();
        String peopleColumn = group.getPeopleColumn();

        String monthsRange = rowWithMonths + ":" + rowWithMonths;
        String peopleListRange = peopleColumn + dataFirstRow + ":" + peopleColumn + dataLastRow;
        String colorStartColumn = Character.valueOf( (char) (peopleColumn.toCharArray()[0]-1)).toString();
        String colorsRange = colorStartColumn + colorsRow + ":" + peopleColumn + (Integer.valueOf(colorsRow) + 10);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Arrays.asList(monthsRange, colorsRange, peopleListRange)).setIncludeGridData(true).execute();

        //TODO retrieve all months and get start end months, get start end columns
        Sheet sheet = spreadsheet.getSheets().get(0);

        Pair<Integer, Integer> startEndColumn = getStartEndColumnForReport(sheet);
        System.out.printf("Total range: [%d : %d] %n", startEndColumn.getKey(), startEndColumn.getValue());
        System.out.printf("Total range: [%s : %s] %n", columnToLetter(startEndColumn.getKey()), columnToLetter(startEndColumn.getValue()));

        GridData gridData = sheet.getData().get(1);
        Map<Marks, Color> colors = parseColors(gridData);

        //TODO get white list rows range
        List<Person> people = new ArrayList<>();
        gridData = sheet.getData().get(2);
        for (int i = 0; i < gridData.getRowData().size(); i++) {
            RowData r = gridData.getRowData().get(i);
            if (r == null || r.getValues() == null) {
                continue;
            }
            CellData cellData = r.getValues().get(0);
            CellFormat effectiveFormat = cellData.getEffectiveFormat();
            if (!effectiveFormat.getTextFormat().getBold() && cellData.getEffectiveValue() != null) {
                Category category;
                if (isWhite(effectiveFormat.getBackgroundColor())) {
                    category = Category.WHITE;
                } else if (isGrey(effectiveFormat.getBackgroundColor())) {
                    category = Category.GUEST;
                } else {
                    category = Category.NEW;
                }
                people.add(new Person(category, cellData.getEffectiveValue().getStringValue(), i));
            }
        }
        //System.out.println("People: " + people);
        List<Person> whiteList = people.stream().filter(p -> p.getCategory() == Category.WHITE).collect(Collectors.toList());

        List<Week> weeks = new ArrayList<>();
        for (int weekIndex = 1; weekIndex <= (startEndColumn.getValue() - startEndColumn.getKey()) / 7; weekIndex++) {
            weeks.add(new Week(leader, weekIndex, whiteList));
        }

        //TODO get week columns and parse by rows by colored lists
        String dataRange = columnToLetter(startEndColumn.getKey()) + dataFirstRow + ":" + columnToLetter(startEndColumn.getValue()) + dataLastRow;
        spreadsheet = service.spreadsheets().get(spreadsheetId).setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();
        List<RowData> dataRows = spreadsheet.getSheets().get(0).getData().get(0).getRowData();
        people.forEach(person -> {
            RowData row = dataRows.get(person.getIndex());
            //TODO parse row by weeks
            handleRow(person, row, weeks, Integer.valueOf(groupDay), colors);
        });

        return weeks;
    }

    private static void printWeeks(Sheets service, String groupLeader, List<Week> weeks, int row) throws IOException {
        prettyPrintWeeks(weeks);

        List<Request> requests = new ArrayList<>();

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest();
        updateCellsRequest.setFields("*");

        GridCoordinate gridCoordinate = new GridCoordinate();
        gridCoordinate.setColumnIndex(0);
        gridCoordinate.setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL)));
        gridCoordinate.setRowIndex(row);
        updateCellsRequest.setStart(gridCoordinate);

        //set header
        List<RowData> allRows = new ArrayList<>();

        // set each row
        Set<String> uniqueNewPeople = new HashSet<>();
        for (Week week : weeks) {
            allRows.add(getWeekRow(week, uniqueNewPeople));
        }
        int lastWhiteCount = weeks.get(weeks.size() - 1).getWhiteList().size();

        //add footer
        allRows.add(getWeekFooterRow(lastWhiteCount, uniqueNewPeople));

        //execute all
        updateCellsRequest.setRows(allRows);
        Request request = new Request();
        request.setUpdateCells(updateCellsRequest);
        requests.add(request);
        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();
    }

    private static RowData getWeekRow(Week week, Set<String> newPeople) {
        RowData rowData = new RowData();

        CellData leader = getCellWithValue(week.getLeader());
        CellData weekName = getCellWithValue(week.getWeekName());
        CellData presentTotal = getCellWithValue(week.getTotalCount());
        CellData listWhite = getCellWithValue(week.getWhiteList().size());
        CellData presentWhite = getCell(week.getPresentByCategory(Category.WHITE).size(), listToNote(week.getWhiteAbsent()));
        CellData presentGuests = getCell(week.getPresentByCategory(Category.GUEST).size(), listToNote(week.getPresentByCategory(Category.GUEST)));
        CellData presentNew = getCell(week.getPresentByCategory(Category.NEW).size(), listToNote(week.getPresentByCategory(Category.NEW)));
        newPeople.addAll(week.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
        CellData groupRate = getCellWithValue("100%");
        CellData visitsWhite = getCellWithValue(week.getVisitWhite());
        CellData meetingsWhite = getCellWithValue(week.getMeetingWhite());
        CellData visitsNew = getCellWithValue(week.getVisitNew());
        CellData meetingsNew = getCellWithValue(week.getMeetingNew());
        CellData callsNew = getCellWithValue(week.getCalls());

        rowData.setValues(Arrays.asList(leader, weekName, listWhite, presentTotal, presentWhite, presentGuests,
                presentNew, groupRate, visitsWhite, meetingsWhite, visitsNew, meetingsNew, callsNew));
        return rowData;
    }

    private static void handleRow(Person person, RowData row, List<Week> weeks, Integer groupDay, Map<Marks, Color> colors) {
        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            List<CellData> weekCells = row.getValues().subList(weekIndex * 7, Math.min(weekIndex * 7 + 7, row.getValues().size()));
            Week week = weeks.get(weekIndex);

            boolean wasPresentOnGroup = areColorsEqual(weekCells.get(groupDay-1).getEffectiveFormat().getBackgroundColor(), colors.get(Marks.GROUP));
            if (wasPresentOnGroup) {
                week.addPresent(person);
            }
            // parse all cells then
            for (CellData cell : weekCells) {
                if (cell.getEffectiveFormat() == null || cell.getEffectiveFormat().getBackgroundColor() == null) continue;
                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
                Marks action = getActionByColor(bgColor, colors);
                week.mergeAction(action, person.getCategory());
            }
        }
    }

    private static Marks getActionByColor(Color color, Map<Marks, Color> colors) {
        for (Map.Entry<Marks, Color> e : colors.entrySet()) {
            if (areColorsEqual(e.getValue(), color)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static Pair<Integer, Integer> getStartEndColumnForReport(Sheet monthsSheet) {
        RowData rowData = monthsSheet.getData().get(0).getRowData().get(0);
        List<CellData> cellDatas = rowData.getValues();

        List<GridRange> merges = monthsSheet.getMerges();
        merges.sort((Comparator.comparing(GridRange::getStartColumnIndex)));

        int initialIndex = merges.size() > 12 ? merges.size() - 12 : 0;
        merges = merges.subList(initialIndex, merges.size());
        Map<String, Pair<Integer, Integer>> months = new HashMap<>();
        for (GridRange merge : merges) {
            int startIndex = merge.getStartColumnIndex();
            String monthName = cellDatas.get(startIndex).getEffectiveValue().getStringValue().toLowerCase();
            months.put(monthName, new Pair<>(merge.getStartColumnIndex(), merge.getEndColumnIndex()));
        }
        int startColumn = months.get(getReportStartMonth()).getKey() + getReportStartDay();
        int endColumn = months.get(getReportEndMonth()).getKey() + getReportEndDay() + 1;
        return new Pair<>(startColumn, endColumn);
    }

    private static Map<Marks, Color> parseColors(GridData gridData) {
        Map<Marks, Color> colors = new HashMap<>();
        for (RowData r : gridData.getRowData()) {
            if (r == null || r.getValues() == null) {
                continue;
            }
            CellData colorCell = r.getValues().get(0);
            CellData nameCell = r.getValues().get(1);
            if (nameCell.getEffectiveValue() == null || colorCell.getEffectiveFormat() == null
                    || colorCell.getEffectiveFormat().getBackgroundColor() == null) {
                continue;
            }
            Color backgroundColor = colorCell.getEffectiveFormat().getBackgroundColor();
            Marks mark = Marks.getEnumFor(nameCell.getEffectiveValue().getStringValue());
            if (mark != null)
                colors.put(mark, backgroundColor);
        }
        return colors;
    }

}