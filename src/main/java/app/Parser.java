package app;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.sun.org.apache.bcel.internal.generic.NEW;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "Google Sheets API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/sheets.googleapis.com-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                Parser.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() throws IOException {
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {
        parsePeriod("1O9zDiEUsYxov30mxtmibVRqW-mCQG7wQ0EXNdC91afg", "4", "81", "84", "4");
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

        // TODO get colors
        GridData gridData = sheet.getData().get(1);
        Map<Marks, Color> colors = parseColors(gridData);
        System.out.println("Colors:" + colors);

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
                Category category = null;
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
        System.out.println("People: " + people);
        List<Person> whiteList = people.stream().filter(p -> p.getCategory() == Category.WHITE).collect(Collectors.toList());

        List<Week> weeks = new ArrayList<>();
        for (int weekIndex = 1; weekIndex <= (startEndColumn.getValue() - startEndColumn.getKey()) / 7 + 1; weekIndex++) {
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

        //TODO print counters
        System.out.println(weeks);
    }

    private static void handleRow(Person person, RowData row, List<Week> weeks, Integer groupDay, Map<Marks, Color> colors) {
        int parsedWeek = 0;
        for (int weekIndex = 1; weekIndex <= weeks.size(); weekIndex++) {
            List<CellData> weekCells = row.getValues().subList(parsedWeek * 7, weekIndex * 7 - 1);
            Week week = weeks.get(weekIndex - 1);

            boolean wasPresentOnGroup = areColorsEqual(weekCells.get(groupDay).getEffectiveFormat().getBackgroundColor(), colors.get(Marks.GROUP));
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
            parsedWeek = weekIndex;
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
        int endColumn = months.get(endMonth).getKey() + Integer.valueOf(endDay);
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