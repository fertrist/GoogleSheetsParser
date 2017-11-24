package app.utils;

import app.entities.Person;
import app.entities.Week;
import app.enums.Actions;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.RowData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportUtil {

    public static boolean isRowEmpty(RowData r) {
        return r == null || r.getValues() == null;
    }

    public static List<Week> getWeeksFromDates(LocalDate start, LocalDate end) {
        List<Week> weeks = new ArrayList<>();
        for (LocalDate tmp = start; tmp.isBefore(end) || tmp.isEqual(end); tmp = tmp.plusWeeks(1)) {
            Week week = new Week();
            week.setStart(tmp);
            week.setEnd(tmp.plusDays(6));
            weeks.add(week);
        }
        return weeks;
    }

    public enum Month {
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
        return join(people.stream().map(Person::getName).collect(Collectors.toList()), "\n");
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
        return color != null && color.getBlue() != null && color.getBlue() == 1.0
                && color.getGreen() != null && color.getGreen() == 1.0
                && color.getRed() != null && color.getRed() == 1.0;
    }

    public static boolean isGrey(Color color) {
        return !isWhite(color) && color.getBlue() != null && color.getGreen() != null && color.getRed() != null
                && color.getBlue().equals(color.getGreen()) && color.getGreen().equals(color.getRed());
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

    public static String getMonthNameFromString(String rawString) {
        rawString = rawString.toLowerCase();
        String month = "";
        for (Month m : Month.values()) {
            if (rawString.contains(m.getName())) {
                month = m.getName();
            }
        }
        return month;
    }

    public static Month getMonthFromString(String s) {

        s = s.toLowerCase();

        for (Month m : Month.values()) {
            if (s.contains(m.getName())) {
                return m;
            }
        }
        return null;
    }

    /**
     * By cell background get kind of action (meeting, visit, call)
     * @param color cell background
     * @param colors color legend
     */
    public static Actions getActionByColor(Color color, Map<Actions, Color> colors) {
        for (Map.Entry<Actions, Color> e : colors.entrySet()) {
            if (areColorsEqual(e.getValue(), color)) {
                return e.getKey();
            }
        }
        return null;
    }

    public static boolean containsIgnoreCase(List<String> names, String name) {
        if (names.contains(name)) {
            return true;
        }
        name = name.replaceAll("\\s+", " ").replaceAll("\\(.*\\)|\\d", "").trim();
        for (String n : names) {
            if (n.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0 || s.trim().length() == 0;
    }

    public static String getDayMonth(LocalDate localDate) {
        return String.format("%02d.%02d", localDate.getDayOfMonth(), localDate.getMonthValue());
    }

    public static boolean hasBackground(CellData cell) {
        return cell.getEffectiveFormat() != null
                && cell.getEffectiveFormat().getBackgroundColor() != null;
    }

    public static String join(Collection collection, String s) {
        StringBuilder builder = new StringBuilder();

        for(Iterator iterator = collection.iterator(); iterator.hasNext(); builder.append((String)iterator.next())) {
            if(builder.length() != 0) {
                builder.append(s);
            }
        }

        return builder.toString();
    }
}
