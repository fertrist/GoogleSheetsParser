package app;

import app.data.CustomSheetApi;
import app.entities.Group;
import app.entities.Person;
import app.enums.Actions;
import app.enums.Category;
import app.utils.ReportUtil;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;
import javafx.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static app.utils.ReportUtil.containsIgnoreCase;
import static app.utils.ReportUtil.isGrey;
import static app.utils.ReportUtil.isRowEmpty;
import static app.utils.ReportUtil.isWhite;

public class PeopleAndColorExtractor
{
    private static final String COLORS_TITLE = "условные обозначения";

    private static CustomSheetApi sheetApi;

    private Group group;

    private List<RowData> rowsWithPeopleAndColors;

    public PeopleAndColorExtractor(Group group) {
        this.group = group;
    }

    public List<RowData> getRowsWithPeopleAndColors() throws IOException
    {
        if (rowsWithPeopleAndColors == null)
        {
            String rangeExpression = sheetApi.getRangeExpression(group.getDataFirstRow(), ReportUtil.MAX_ROWS, group.getPeopleColumnAsNumber(), group.getPeopleColumn());
            rowsWithPeopleAndColors = sheetApi.getRowsData(group.getSpreadSheetId(), rangeExpression);
        }
        return rowsWithPeopleAndColors;
    }

    public static void setSheetApi(CustomSheetApi sheetApi)
    {
        PeopleAndColorExtractor.sheetApi = sheetApi;
    }

    public List<Person> getPeople() throws IOException
    {
        return parsePeopleFromRows(getRowsWithPeopleAndColors());
    }

    /**
     * Retrieves people by categories
     */
    private List<Person> parsePeopleFromRows(List<RowData> peopleData) {
        List<Person> people = new ArrayList<>();

        int offset = group.getDataFirstRow() - 1;

        for (int i = 0; i < peopleData.size(); i++) {

            RowData r = peopleData.get(i);

            if (isRowEmpty(r)) continue;
            if (r.getValues().size() < 2) continue; // should be at least 2 columns

            CellData cellData = r.getValues().get(1); // 2nd column is column with people names

            if (isColorsTitle(cellData)) return people;

            if (!isUnderline(cellData) && !isCellEmpty(cellData)) {
                Category category = defineCategory(cellData, group);
                people.add(new Person(category, cellData.getEffectiveValue().getStringValue(), offset + i));
            }
        }

        return people;
    }

    private Category defineCategory(CellData cellData, Group group) {

        CellFormat effectiveFormat = cellData.getEffectiveFormat();

        String name = cellData.getEffectiveValue().getStringValue();
        Category category;

        // handle added/removed people (they first are considered as if they weren't added/removed)
        boolean isAdded = containsIgnoreCase(group.getAddedPeople(), name);
        boolean isRemoved = containsIgnoreCase(group.getRemovedPeople(), name);
        boolean onTrial = name.toLowerCase().contains("(и.с") || name.toLowerCase().contains("(исп.срок)")
                || name.toLowerCase().contains("(исп");

        if ((isWhite(effectiveFormat.getBackgroundColor()) && !isAdded && !onTrial) || isRemoved) {
            category = Category.WHITE;
        }
        else if (isGrey(effectiveFormat.getBackgroundColor()) && !onTrial) {
            category = Category.GUEST;
        }
        else if (isGrey(effectiveFormat.getBackgroundColor()) && onTrial) {
            category = Category.TRIAL;
        }
        else {
            category = Category.NEW;
        }

        return category;
    }

    public static final Integer DATA_OVERLAP = 30;

    public static Map<Actions, Color> parseColors(List<RowData> rows, int colorsRow) {
        Map<Actions, Color> colors = new HashMap<>();
        for (int j = colorsRow + 1; j < Math.min(colorsRow + DATA_OVERLAP, rows.size()); j++) {
            RowData r = rows.get(j);

            if (isRowEmpty(r)) continue;

            CellData colorCell = r.getValues().get(0);
            CellData nameCell = r.getValues().get(1);

            if (isCellEmpty(nameCell) ||
                    (colorCell.getEffectiveFormat() == null
                            || colorCell.getEffectiveFormat().getBackgroundColor() == null)) {
                break;
            }

            Color backgroundColor = colorCell.getEffectiveFormat().getBackgroundColor();
            Actions mark = Actions.getEnumFor(nameCell.getEffectiveValue().getStringValue());
            if (mark != null)
                colors.putIfAbsent(mark, backgroundColor);
        }
        return colors;
    }

    public static Pair<Integer, Integer> getLastDataAndColorsRow(List<RowData> rows, int offsetFromStart) {
        int lastDataRow = 0;
        int colorsRow = 0;

        for (int i = offsetFromStart; i < rows.size(); i++) {

            RowData r = rows.get(i);

            if (isRowEmpty(r)) continue;

            CellData cell = r.getValues().get(Math.min(r.getValues().size() - 1, 1));

            if (isCellEmpty(cell)) continue;

            if (isColorsTitle(cell)) {
                colorsRow = i;
                break;
            }

            lastDataRow = i;
        }
        return new Pair<>(lastDataRow + offsetFromStart, colorsRow + offsetFromStart);
    }

    public Map<Actions, Color> extractColors() throws IOException
    {
        int dataOffset = group.getDataFirstRow();

        Pair<Integer, Integer> dataColorRows = getLastDataAndColorsRow(getRowsWithPeopleAndColors(), dataOffset);
        int colorsRow = dataColorRows.getValue();
        int lastDataRow = dataColorRows.getKey();

        Map<Actions, Color> colors = parseColors(getRowsWithPeopleAndColors(), colorsRow - dataOffset);

        return colors;
    }

    public List<RowData> getData(Pair<Integer, Integer> reportColumns) throws IOException {
        int dataOffset = group.getDataFirstRow();

        Pair<Integer, Integer> dataColorRows = getLastDataAndColorsRow(getRowsWithPeopleAndColors(), dataOffset);

        int lastDataRow = dataColorRows.getKey();

        return sheetApi.getRowsData(group.getSpreadSheetId(),
                group.getRowWithMonths(), lastDataRow, reportColumns.getKey(), reportColumns.getValue());
    }

    private static boolean isColorsTitle(CellData cellData) {
        return isUnderline(cellData) && cellData.getEffectiveValue() != null
                && cellData.getEffectiveValue().getStringValue().equalsIgnoreCase(COLORS_TITLE);
    }

    private static boolean isUnderline(CellData cellData) {
        return cellData.getEffectiveFormat() != null && cellData.getEffectiveFormat().getTextFormat().getUnderline();
    }

    private static boolean isCellEmpty(CellData cellData) {
        return cellData.getEffectiveValue() == null || cellData.getEffectiveValue().getStringValue() == null
                || cellData.getEffectiveValue().getStringValue().isEmpty();
    }
}
