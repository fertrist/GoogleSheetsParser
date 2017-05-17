package app;

import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GroupTablesParser extends GoogleSheetsApp {

    public static void main(String[] args) throws IOException {
        parsePeriod("1O9zDiEUsYxov30mxtmibVRqW-mCQG7wQ0EXNdC91afg", "4", "81", "84", "4");

        //populateReport(service, spreadsheetId, weeks);
    }

    public static void parsePeriod(String spreadsheetId, String dataStartRow, String dataEndRow, String markingRow, String groupDay) throws IOException {
        Sheets service = getSheetsService();
        String monthsRange = "1:1";
        String peopleListRange = "B" + dataStartRow + ":B" + dataEndRow;
        String colorsRange = "B" + markingRow + ":B" + (Integer.valueOf(markingRow) + 3);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Arrays.asList(monthsRange, colorsRange, peopleListRange)).setIncludeGridData(true).execute();

        //TODO retrieve all months and get start end months, get start end columns
        Sheet sheet = spreadsheet.getSheets().get(0);
        Pair<Integer, Integer> startEndColumn = getStartEndColumnForReport(sheet);
        System.out.printf("Total range: [%d : %d] %n", startEndColumn.getKey(), startEndColumn.getValue());
        System.out.printf("Total range: [%s : %s] %n", columnToLetter(startEndColumn.getKey()), columnToLetter(startEndColumn.getValue()));

        // TODO get colors
        GridData gridData = sheet.getData().get(1);
        Map<Marks, Color> colors = parseColors(gridData);
        //System.out.println("Colors:" + colors);

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
            weeks.add(new Week(weekIndex, whiteList));
        }

        //TODO get week columns and parse by rows by colored lists
        String dataRange = columnToLetter(startEndColumn.getKey()) + dataStartRow + ":" + columnToLetter(startEndColumn.getValue()) + dataEndRow;
        spreadsheet = service.spreadsheets().get(spreadsheetId).setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();
        List<RowData> dataRows = spreadsheet.getSheets().get(0).getData().get(0).getRowData();
        people.forEach(person -> {
            RowData row = dataRows.get(person.getIndex());
            //TODO parse row by weeks
            handleRow(person, row, weeks, Integer.valueOf(groupDay), colors);
        });

        //TODO print counters / create report
        populateReport(service, spreadsheetId, weeks);
    }

    /*private static void test() {
        String spreadsheetId = "1iV-5GzXelLaazFvVVa9DH5uV7YENi7GNSXgvXEgkeZM";
        List<Request> requests = new ArrayList<>();
        // Change the spreadsheet's title.
        requests.add(new Request()
                .setUpdateSpreadsheetProperties(new UpdateSpreadsheetPropertiesRequest()
                        .setProperties(new SpreadsheetProperties()
                                .setTitle("Ololo"))
                        .setFields("title")));
        // Find and replace text.
        requests.add(new Request()
                .setFindReplace(new FindReplaceRequest()
                        .setFind(find)
                        .setReplacement(replacement)
                        .setAllSheets(true)));
        // Add additional requests (operations) ...

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        BatchUpdateSpreadsheetResponse response =
                service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
        FindReplaceResponse findReplaceResponse = response.getReplies().get(1).getFindReplace();
        System.out.printf("%d replacements made.", findReplaceResponse.getOccurrencesChanged());
    }*/

        /*GridRange gridRange = new GridRange();
        gridRange.setSheetId(542076770);
        gridRange.setStartColumnIndex(1);
        gridRange.setStartRowIndex(1);
        updateCellsRequest.setRange(gridRange);*/


    private static void populateReport(Sheets service, String spreadsheetId, List<Week> weeks) throws IOException {
        prettyPrintWeek(weeks);
        spreadsheetId = "1iV-5GzXelLaazFvVVa9DH5uV7YENi7GNSXgvXEgkeZM"; //test sheet

        List<Request> requests = new ArrayList<>();

        //TODO set header
        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest();
        updateCellsRequest.setFields("*");

        GridCoordinate gridCoordinate = new GridCoordinate();
        gridCoordinate.setColumnIndex(1);
        gridCoordinate.setSheetId(542076770);
        gridCoordinate.setRowIndex(1);
        updateCellsRequest.setStart(gridCoordinate);

        RowData rowData = new RowData();
        List<CellData> cellDatas = new ArrayList<>();
        String [] reportColumns = new String[]{"Неделя", "По списку", "Было всего", "Cписочных", "Гости", "Новые люди",
                "Как прошла гр.(%)", "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки"};
        for (String reportColumn : reportColumns) {
            CellData cellData = new CellData();

            ExtendedValue extendedValue = new ExtendedValue();
            extendedValue.setStringValue(reportColumn);

            cellData.setUserEnteredValue(extendedValue);
            cellData.setNote("fdjkfkjflds");

            CellFormat cellFormat = new CellFormat();
            cellFormat.setBackgroundColor(new Color().setRed(new Float(0.5)));

            cellData.setUserEnteredFormat(cellFormat);
            cellDatas.add(cellData);
        }
        rowData.setValues(cellDatas);
        updateCellsRequest.setRows(Collections.singletonList(rowData));


        // TODO set each row ...


        // TODO execute all
        Request request = new Request();
        request.setUpdateCells(updateCellsRequest);
        requests.add(request);
        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
    }

    private static void prettyPrintWeek(List<Week> weeks) {
        String format = "%10s | %10s | %10s | %10s | %10s | %10s | %15s | %15s | %15s | %15s | %10s %n";
        System.out.printf(format, "Неделя", "По списку", "Было всего", "Cписочных", "Гости", "Новые люди",
                "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки");
        for (Week week : weeks) {
            System.out.printf(format, week.getWeekName(), week.getWhiteList().size(), week.getPresent().size(), week.getPresentByCategory(Category.WHITE).size(),
                    week.getPresentByCategory(Category.GUEST).size(), week.getPresentByCategory(Category.NEW).size(), week.getVisitWhite(),
                    week.getMeetingWhite(), week.getVisitNew(), week.getMeetingNew(), week.getCalls());
        }
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

    private static boolean areColorsEqual(Color color1, Color color2) {
        return Objects.equals(color1.getBlue(), color2.getBlue())
                && Objects.equals(color1.getRed(), color2.getRed())
                && Objects.equals(color1.getGreen(), color2.getGreen());
    }

    private static Pair<Integer, Integer> getStartEndColumnForReport(Sheet monthsSheet) {
        String startDay = "3";
        String startMonth = "апрель";
        String endDay = "30";
        String endMonth = "апрель";
        RowData rowData = monthsSheet.getData().get(0).getRowData().get(0);
        List<CellData> cellDatas = rowData.getValues();
        for (CellData cell : cellDatas) {
            if (cell.size() != 0 && cell.getEffectiveValue() != null) {
                System.out.println(cell.getEffectiveValue().getStringValue());
            }
        }
        List<GridRange> merges = monthsSheet.getMerges();
        merges.sort((Comparator.comparing(GridRange::getStartColumnIndex)));
        int initialIndex = merges.size() > 12 ? merges.size() - 12 : 0;
        merges = merges.subList(initialIndex, merges.size());
        Map<String, Pair<Integer, Integer>> months = new HashMap<>();
        for (GridRange merge : merges) {
            int startIndex = merge.getStartColumnIndex();
            String monthName = cellDatas.get(startIndex).getEffectiveValue().getStringValue().toLowerCase();
            months.put(monthName, new Pair<>(merge.getStartColumnIndex(),merge.getEndColumnIndex()));
        }
        int startColumn = months.get(startMonth).getKey() + Integer.valueOf(startDay);
        int endColumn = months.get(endMonth).getKey() + Integer.valueOf(endDay) + 1;
        return new Pair<>(startColumn, endColumn);
    }

    private static boolean isWhite(Color color) {
        return color.getBlue() == 1.0 && color.getGreen() == 1.0 && color.getRed() == 1.0;
    }

    private static boolean isGrey(Color color) {
        return color.getBlue().equals(color.getGreen()) && color.getGreen().equals(color.getRed());
    }

    private static Map<Marks, Color> parseColors(GridData gridData) {
        Map<Marks, Color> colors = new HashMap<>();
        for (RowData r : gridData.getRowData()) {
            if (r == null || r.getValues() == null) {
                continue;
            }
            CellData cellData = r.getValues().get(0);
            CellFormat effectiveFormat = cellData.getEffectiveFormat();
            colors.put(Marks.getEnumFor(cellData.getEffectiveValue().getStringValue()), effectiveFormat.getBackgroundColor());
        }
        return colors;
    }

    private static String columnToLetter(int column) {
        if (column < 26) {
            return Character.toString((char) (64 + column));
        }
        int temp;
        String letter = "";
        while (column > 0)
        {
            temp = (column - 1) % 26;
            letter = Character.toString((char) (temp + 65)) + letter;
            column = (column - temp - 1) / 26;
        }
        return letter;
    }

}