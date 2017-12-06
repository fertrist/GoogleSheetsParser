package app.extract;

import static app.extract.ReportUtil.containsIgnoreCase;
import app.entities.Category;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.entities.Group;

public class PersonCategoryFinder
{
    private Group group;

    public PersonCategoryFinder(Group group) {
        this.group = group;
    }

    public Category defineCategory(CellWrapper cellWrapper) {
        String name = cellWrapper.getStringValue();
        ColorWrapper colorWrapper = cellWrapper.getBgColor();

        // handle added/removed people (they first are considered as if they weren't added/removed)
        boolean isAdded = containsIgnoreCase(group.getAddedPeople(), name);
        boolean isRemoved = containsIgnoreCase(group.getRemovedPeople(), name);
        boolean onTrial = name.toLowerCase().contains("(и.с") || name.toLowerCase().contains("(исп.срок)")
                || name.toLowerCase().contains("(исп");

        Category category;
        if ((colorWrapper.isWhite() && !isAdded && !onTrial) || isRemoved) {
            category = Category.WHITE;
        }
        else if (colorWrapper.isGrey() && !onTrial) {
            category = Category.GUEST;
        }
        else if (colorWrapper.isGrey()) {
            category = Category.TRIAL;
        }
        else {
            category = Category.NEW;
        }

        return category;
    }
}
