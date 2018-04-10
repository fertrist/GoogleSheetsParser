package app.extract;

import app.entities.Category;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.entities.Group;
import org.apache.commons.lang3.StringUtils;

public class PersonCategoryFinder
{
    private Group group;
    private String personName;
    private ColorWrapper background;

    public PersonCategoryFinder(Group group, CellWrapper cellWrapper) {
        this.group = group;
        this.personName = cellWrapper.getStringValue().trim();
        this.background = cellWrapper.getBgColor();
    }

    public Category defineCategory() {
        if (canBeSetToWhiteCategory())
        {
            return Category.WHITE;
        }
        return defineNotWhiteCategory();
    }

    private boolean canBeSetToWhiteCategory()
    {
        return hasBeenJustRemoved() // means person was not removed on first weeks
                // has white bg, is not trial person, and was not added only on the last week
                || (background.isWhite() && !isOnTrial() && !hasBeenJustAdded());
    }

    private boolean isOnTrial()
    {
        String lowerCase = personName.toLowerCase();
        return lowerCase.contains("(и.с") || lowerCase.contains("(исп")
                || lowerCase.contains("(ис") || lowerCase.contains("срок)")
                || lowerCase.contains("(випр") || lowerCase.contains("терм");
    }

    private boolean hasBeenJustRemoved()
    {
        return group.getRemovedPeople().stream()
                .filter(value -> StringUtils.equalsIgnoreCase(value, personName))
                .findFirst().isPresent();
    }

    private boolean hasBeenJustAdded()
    {
        return group.getAddedPeople().stream()
                .filter(value -> StringUtils.equalsIgnoreCase(value, personName))
                .findFirst().isPresent();
    }

    private Category defineNotWhiteCategory()
    {
        Category notWhileCategory;
        if (isOnTrial())
        {
            notWhileCategory = Category.TRIAL;
        }
        else if (background.isGrey())
        {
            notWhileCategory = Category.GUEST;
        }
        else
        {
            notWhileCategory = Category.NEW;
        }
        return notWhileCategory;
    }
}
