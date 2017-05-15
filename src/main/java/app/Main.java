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
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
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
                Main.class.getResourceAsStream("/client_secret.json");
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
        parseWeek();
    }

    public static void parseWeek() throws IOException {
        String groupDay = "4";
        String dataStartRow = "4";
        String dataEndRow = "81";
        String marking = "84";
        Sheets service = getSheetsService();
        String spreadsheetId = "1O9zDiEUsYxov30mxtmibVRqW-mCQG7wQ0EXNdC91afg";
        String monthsRange = "1:1";
        String peopleListRange = "B" + dataStartRow + ":B" + dataEndRow;
        String colorsRange = "B" + marking + ":B" + (Integer.valueOf(marking) + 3);

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
        List<String> whiteList = new ArrayList<>();
        List<String> newPeople = new ArrayList<>();
        List<String> guests = new ArrayList<>();
        gridData = sheet.getData().get(2);
        for (int i = 0; i < gridData.getRowData().size(); i++) {
            RowData r = gridData.getRowData().get(i);
            if (r == null || r.getValues() == null) {
                continue;
            }
            CellData cellData = r.getValues().get(0);
            CellFormat effectiveFormat = cellData.getEffectiveFormat();
            if (!effectiveFormat.getTextFormat().getBold() && cellData.getEffectiveValue() != null) {
                if (isWhite(effectiveFormat.getBackgroundColor())) {
                    whiteList.add(i, cellData.getEffectiveValue().getStringValue());
                } else if (isGrey(effectiveFormat.getBackgroundColor())) {
                    guests.add(cellData.getEffectiveValue().getStringValue());
                } else {
                    newPeople.add(cellData.getEffectiveValue().getStringValue());
                }
            }
        }
        System.out.println("White list: " + whiteList);
        System.out.println("New people: " + newPeople);
        System.out.println("Guests: " + guests);

        List<Week> weeks = new ArrayList<>();
        for (int weekIndex = 1; weekIndex <= (startEndColumn.getValue() - startEndColumn.getKey()) / 7 + 1; weekIndex++) {
            weeks.add(new Week(weekIndex));
        }

        //TODO get week columns and parse by rows by colored lists
        String dataRange = columnToLetter(startEndColumn.getKey()) + dataStartRow + ":" + columnToLetter(startEndColumn.getValue()) + dataEndRow;
        spreadsheet = service.spreadsheets().get(spreadsheetId).setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();
        List<RowData> dataRows = spreadsheet.getSheets().get(0).getData().get(0).getRowData();
        dataRows.forEach(r -> {
            int parsedWeek = 0;
            for (int weekIndex = 1; weekIndex <= weeks.size(); weekIndex++) {
                List<CellData> weekCells = r.getValues().subList(parsedWeek * 7, weekIndex * 7 - 1);
                Week week = weeks.get(weekIndex-1);
                handleWeekCells(week, weekCells, colors);
                parsedWeek = weekIndex;
            }
        });

        //TODO print counters
    }

    private static void handleWeekCells(Week week, List<CellData> weekCells, Map<Marks, Color> colors) {
        for (CellData cell : weekCells) {
            Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
            if (bgColor.equals(colors.get(Marks.GROUP))) {

            }
        }
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
            colors.put(Marks.valueOf(cellData.getEffectiveValue().getStringValue()), effectiveFormat.getBackgroundColor());
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

    private enum Counters {
        GROUP_TOTAL, GROUP_WHITE, GROUP_NEW, GROUP_GUESTS, VISIT_WHITE, VISIT_NEW, MEETING_WHITE, MEETING_NEW, CALL;
    }

    private enum Marks {
        GROUP("группа"), VISIT("посещение"), MEETING("встреча"), CALL("звонок");

        private String name;

        Marks(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static class Week {
        int weekNumber;
        int groupNew = 0;
        int groupWhite = 0;
        int meetingNew = 0;
        int meetingWhite = 0;
        int visitNew = 0;
        int visitWhite = 0;
        List<ArrayList> absentWhite = new ArrayList<>();
        List<ArrayList> newPeople = new ArrayList<>();
        List<ArrayList> guests = new ArrayList<>();

        public Week(int weekNumber) {this.weekNumber = weekNumber;}

        public int getGroupNew() {
            return groupNew;
        }

        public void setGroupNew(int groupNew) {
            this.groupNew = groupNew;
        }

        public int getGroupWhite() {
            return groupWhite;
        }

        public void setGroupWhite(int groupWhite) {
            this.groupWhite = groupWhite;
        }

        public int getMeetingNew() {
            return meetingNew;
        }

        public void setMeetingNew(int meetingNew) {
            this.meetingNew = meetingNew;
        }

        public int getMeetingWhite() {
            return meetingWhite;
        }

        public void setMeetingWhite(int meetingWhite) {
            this.meetingWhite = meetingWhite;
        }

        public int getVisitNew() {
            return visitNew;
        }

        public void setVisitNew(int visitNew) {
            this.visitNew = visitNew;
        }

        public int getVisitWhite() {
            return visitWhite;
        }

        public void setVisitWhite(int visitWhite) {
            this.visitWhite = visitWhite;
        }

        public List<ArrayList> getAbsentWhite() {
            return absentWhite;
        }

        public void setAbsentWhite(List<ArrayList> absentWhite) {
            this.absentWhite = absentWhite;
        }

        public List<ArrayList> getNewPeople() {
            return newPeople;
        }

        public void setNewPeople(List<ArrayList> newPeople) {
            this.newPeople = newPeople;
        }

        public List<ArrayList> getGuests() {
            return guests;
        }

        public void setGuests(List<ArrayList> guests) {
            this.guests = guests;
        }
    }
}