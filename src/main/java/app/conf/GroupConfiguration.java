package app.conf;

import app.entities.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupConfiguration {

    private List<Group> groups;

    public GroupConfiguration()
    {

    }

    private void init() {
        groups = new ArrayList<>();
        groups.add(Group.builder().build());

        /*return Group.builder().groupNumber(groupOrdinalNumber).spreadSheetId(spreadsheetId)
                .leaderName(leaderName)
                .groupDay(groupWeekDay)
                .monthsRow(rowWithMonths)
                .rowWithDates(rowWithDates)
                .rowWithDays(rowWithDays)
                .peopleColumn(peopleColumn)
                .dataStartRow(dataFirstRow)
                .addedPeople(addedPeople)
                .removedPeople(removedPeople)
                .build();*/
    }

}
