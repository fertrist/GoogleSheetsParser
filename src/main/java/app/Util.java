package app;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.sun.deploy.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Util {

    private enum Month {
        JAN("январь"), FEB("февраль"), MAR("март"),
        APR("апрель"), MAY("май"), JUN("июнь"),
        JUL("июль"), AUG("август"), SEP("сентябрь"),
        OCT("октябрь"), NOV("ноябрь"), DEC("декабрь");

        private String translation;

        Month(String translation) {
            this.translation = translation;
        }

        public String getName() {
            return translation;
        }
    }

    public static String listToNote(List<Person> people) {
        return StringUtils.join(people.stream().map(Person::getName).collect(Collectors.toList()), "\n");
    }

    public static CellData getCell(int value, String note) {
        return getCellWithValue(value).setNote(note);
    }

    public static CellData getCellWithValue(String value) {
        CellData cellData = new CellData();
        cellData.setUserEnteredValue(new ExtendedValue().setStringValue(value));
        return cellData;
    }

    public static CellData getCellWithValue(int value) {
        CellData cellData = new CellData();
        cellData.setUserEnteredValue(new ExtendedValue().setNumberValue((double) value));
        return cellData;
    }

    public static CellData getCellWithBgColor(String value, Color color) {
        CellData cellData = new CellData();
        cellData.setUserEnteredValue(new ExtendedValue().setStringValue(value));
        cellData.setUserEnteredFormat(new CellFormat().setBackgroundColor(color));
        return cellData;
    }

    public static CellData getCellWithBgColor(Color color) {
        CellData cellData = new CellData();
        cellData.setUserEnteredFormat(new CellFormat().setBackgroundColor(color));
        return cellData;
    }

    public static CellData getCellWithBgColor(int value, Color color) {
        CellData cellData = new CellData();
        cellData.setUserEnteredValue(new ExtendedValue().setNumberValue((double) value));
        cellData.setUserEnteredFormat(new CellFormat().setBackgroundColor(color));
        return cellData;
    }

    public static CellData getCellWithBgColor(int red, int green, int blue) {
        CellData cellData = new CellData();
        cellData.setUserEnteredFormat(new CellFormat().setBackgroundColor(getColor(red, green, blue)));
        return cellData;
    }

    public static CellData getCellWithBgColor(String value, javafx.scene.paint.Color color) {
        CellData cellData = new CellData();
        cellData.setUserEnteredValue(new ExtendedValue().setStringValue(value));
        Color cellColor = new Color();
        cellColor.setRed((float) color.getRed());
        cellColor.setGreen((float) color.getGreen());
        cellColor.setBlue((float) color.getBlue());
        cellData.setUserEnteredFormat(new CellFormat().setBackgroundColor(cellColor));
        return cellData;
    }

    public static Color getColor(int red, int green, int blue) {
        Color color = new Color();
        color.setRed((float) (red / 255.0));
        color.setGreen((float) (green / 255.0));
        color.setBlue((float) (blue / 255.0));
        return color;
    }

    public static boolean areColorsEqual(Color color1, Color color2) {
        return (color1 == color2) || (color1 != null && color2 != null)
                && Objects.equals(color1.getBlue(), color2.getBlue())
                && Objects.equals(color1.getRed(), color2.getRed())
                && Objects.equals(color1.getGreen(), color2.getGreen());
    }

    public static boolean isWhite(Color color) {
        return color.getBlue() == 1.0 && color.getGreen() == 1.0 && color.getRed() == 1.0;
    }

    public static boolean isGrey(Color color) {
        return color.getBlue().equals(color.getGreen()) && color.getGreen().equals(color.getRed());
    }

    public static String columnToLetter(int column) {
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

    public static String getWeekDay(int day) {
        String weekDay = null;
        switch (day) {
            case 1:
                weekDay = "пн";
                break;
            case 2:
                weekDay = "вт";
                break;
            case 3:
                weekDay = "ср";
                break;
            case 4:
                weekDay = "чт";
                break;
            case 5:
                weekDay = "пт";
                break;
            case 6:
                weekDay = "сб";
                break;
            case 7:
                weekDay = "вс";
                break;

        }
        return weekDay;
    }

    public static int getMonthNumber(String month) {
        int number = 0;
        switch (month.toLowerCase()) {
            case "январь":
                number = 1;
                break;
            case "февраль":
                number = 2;
                break;
            case "март":
                number = 3;
                break;
            case "апрель":
                number = 4;
                break;
            case "май":
                number = 5;
                break;
            case "июнь":
                number = 6;
                break;
            case "июль":
                number = 7;
                break;
            case "август":
                number = 8;
                break;
            case "сентябрь":
                number = 9;
                break;
            case "октябрь":
                number = 10;
                break;
            case "ноябрь":
                number = 11;
                break;
            case "декабрь":
                number = 12;
                break;
        }
        return number;
    }

    public static String getMonthFromString(String rawString) {
        rawString = rawString.toLowerCase();
        String month = "";
        for (Month m : Month.values()) {
            if (rawString.contains(m.getName())) {
                month = m.getName();
            }
        }
        return month;
    }

    static int findMonthForColumn(Map<String, List<Integer>> map, int c) {
        String month = null;
        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
            if (entry.getValue().contains(c)) month = entry.getKey();
        }
        return getMonthNumber(month);
    }
}
