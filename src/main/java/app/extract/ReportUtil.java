package app.extract;

import app.entities.Person;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.RowData;
import org.mockito.internal.util.collections.Sets;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportUtil {

    public static final int MAX_ROWS = 120;

    public static boolean isRowEmpty(RowData r) {
        return r == null || r.getValues() == null;
    }

    public enum Month {
        JAN("январь", "січень"), FEB("февраль", "лютий"), MAR("март", "березень"),
        APR("апрель", "квітень"), MAY("май", "травень"), JUN("июнь", "червень"),
        JUL("июль", "липень"), AUG("август", "серпень"), SEP("сентябрь", "вересень"),
        OCT("октябрь", "жовтень"), NOV("ноябрь", "листопад"), DEC("декабрь", "грудень");

        private Set<String> translations;

        Month(String... translations) {
            this.translations = Sets.newSet(translations);
        }

        public Set<String> getTranslations() {
            return translations;
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

    public static String columnToLetter(int column) {
        if (column < 26) {
            return Character.toString((char) (64 + column));
        }
        int temp;
        String letter = "";
        while (column > 0)
        {
            temp = (column - 1) % 26;
            letter = (char) (temp + 65) + letter;
            column = (column - temp - 1) / 26;
        }
        return letter;
    }

    private static boolean anyTranslationMatchesString(Set<String> monthTranslations, String str) {
        for (String translation : monthTranslations) {
            if (translation.toLowerCase().contains(str.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static Month constructMonthFromName(String s) {

        s = s.toLowerCase();

        for (Month m : Month.values()) {
            if (anyTranslationMatchesString(m.getTranslations(), s)) {
                return m;
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
