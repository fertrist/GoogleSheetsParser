package app.extract;

import static app.extract.ReportUtil.isRowEmpty;
import app.dao.GroupSheetApi;
import app.entities.Category;
import app.entities.CellWrapper;
import app.entities.Person;
import com.google.api.services.sheets.v4.model.RowData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeopleExtractor
{
    private GroupSheetApi groupSheetApi;

    private List<RowData> rowsWithPeopleAndColors;


    public PeopleExtractor(GroupSheetApi groupSheetApi) {
        this.groupSheetApi = groupSheetApi;
    }

    public List<Person> extractPeople() throws IOException
    {
        List<RowData> peopleAndColors = groupSheetApi.getRowsWithPeopleAndColors();
        return parsePeopleFromRows(peopleAndColors);
    }

    /**
     * Retrieves people by categories
     */
    private List<Person> parsePeopleFromRows(List<RowData> peopleData) {
        List<Person> people = new ArrayList<>();

        int offset = groupSheetApi.getGroup().getDataFirstRow() - 1;

        for (int i = 0; i < peopleData.size(); i++) {

            RowData r = peopleData.get(i);

            if (isRowEmpty(r)) continue;
            if (r.getValues().size() < 2) continue; // should be at least 2 columns

            CellWrapper cellWrapper = new CellWrapper(r.getValues().get(1)); // 2nd column is column with people names

            if (cellWrapper.isColorsTitle()) return people;

            if (!cellWrapper.isUnderline() && !cellWrapper.isCellEmpty()) {
                PersonCategoryFinder categoryFinder = new PersonCategoryFinder(groupSheetApi.getGroup());

                Category category = categoryFinder.defineCategory(cellWrapper);

                people.add(new Person(category, cellWrapper.getStringValue(), offset + i));
            }
        }

        return people;
    }




}
