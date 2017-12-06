package app.extract;

import static app.extract.ReportUtil.isRowEmpty;
import app.dao.GroupSheetApi;
import app.data.ColorActionMapper;
import app.entities.Action;
import app.entities.CellWrapper;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;
import javafx.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorExtractor
{
    public static final Integer DATA_OVERLAP = 30;
    private GroupSheetApi groupSheetApi;

    public ColorExtractor(GroupSheetApi groupSheetApi) {
        this.groupSheetApi = groupSheetApi;
    }

    public ColorActionMapper getColorActionMapper() throws IOException
    {
        List<RowData> peopleAndColors = groupSheetApi.getRowsWithPeopleAndColors();
        Pair<Integer, Integer> dataColorRows = groupSheetApi.getLastDataAndColorsRow(peopleAndColors);
        int colorsRow = dataColorRows.getValue();
        int lastDataRow = dataColorRows.getKey();

        Map<Action, Color> colors = parseColors(peopleAndColors, colorsRow - groupSheetApi.getGroup().getDataFirstRow());
        return new ColorActionMapper(colors);
    }

    private static Map<Action, Color> parseColors(List<RowData> rows, int colorsRow) {
        Map<Action, Color> colors = new HashMap<>();
        for (int j = colorsRow + 1; j < Math.min(colorsRow + DATA_OVERLAP, rows.size()); j++) {
            RowData r = rows.get(j);

            if (isRowEmpty(r)) continue;

            CellData colorCell = r.getValues().get(0);
            CellWrapper nameCell = new CellWrapper(r.getValues().get(1));


            if (nameCell.isCellEmpty() ||
                    (colorCell.getEffectiveFormat() == null
                            || colorCell.getEffectiveFormat().getBackgroundColor() == null)) {
                break;
            }

            Color backgroundColor = colorCell.getEffectiveFormat().getBackgroundColor();
            Action mark = Action.getEnumFor(nameCell.getStringValue());
            if (mark != null)
                colors.putIfAbsent(mark, backgroundColor);
        }
        return colors;
    }
}
