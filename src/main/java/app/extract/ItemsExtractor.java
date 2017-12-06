package app.extract;

import static app.extract.ReportUtil.hasBackground;
import static app.extract.ReportUtil.isRowEmpty;
import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.Action;
import app.entities.Person;
import app.report.ReportItem;
import app.report.ReportRange;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ItemsExtractor
{
    private GroupTableData groupTableData;

    public ItemsExtractor(GroupTableData groupTableData) {
        this.groupTableData = groupTableData;
    }

    public List<ReportItem> getItems()
    {
        List<ReportItem> reportItems = new ArrayList<>();
        ReportRange reportRange = groupTableData.getReportRange();
        int diff = reportRange.getStart().getColumn() - reportRange.getEnd().getColumn();

        List<RowData> dataRows = groupTableData.getData();
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();
        List<Person> people = groupTableData.getPeople();
        ColorActionMapper colorActionMapper = groupTableData.getColorActionMapper();

        for (Person person : people)
        {
            // case where row is empty for the person thus not fetched
            if (dataRows.size() < person.getIndex()+1)
            {
                continue;
            }
            RowData row = dataRows.get(person.getIndex());

            if (isRowEmpty(row)) continue;

            List<CellData> personCells = row.getValues();
            /*List<CellData> personCells = row.getValues().subList(0, diff).stream().filter(ReportUtil::hasBackground)
                    .collect(Collectors.toList());*/

            for (int i = 0; i < personCells.size(); i++)
            {
                CellData cell = personCells.get(i);

                if (!hasBackground(cell)) continue;

                Color bgColor = cell.getEffectiveFormat().getBackgroundColor();


                Action action = colorActionMapper.getActionByColor(bgColor);

                if (action != null)
                {
                    LocalDate date = columnDateMapper.dateForColumn(i);
                    reportItems.add(new ReportItem(person.clone(), action, date));
                }
            }
        }
        return reportItems;
    }
}
