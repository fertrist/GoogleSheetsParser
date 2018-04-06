package app.extract;

import app.entities.Category;
import app.entities.CellWrapper;
import app.entities.ColorWrapper;
import app.entities.Group;
import org.apache.commons.lang3.StringUtils;

public class PersonCategoryFinder
{
    private Group group;

    public PersonCategoryFinder(Group group) {
        this.group = group;
    }

    public Category defineCategory(CellWrapper cellWrapper) {
        String personName = cellWrapper.getStringValue().trim();
        ColorWrapper colorWrapper = cellWrapper.getBgColor();

        boolean isOnTrial = isOnTrial(personName);
        if (hasBeenJustRemoved(personName) // means it was not removed on first n weeks
                || (colorWrapper.isWhite() && !isOnTrial && !hasBeenJustAdded(personName)))
        {
            return Category.WHITE;
        }
        return defineNotWhiteCategory(colorWrapper.isGrey(), isOnTrial);
    }

    private boolean isOnTrial(String personName)
    {
        String lowerCase = personName.toLowerCase();
        return lowerCase.contains("(и.с") || lowerCase.contains("(исп")
                || lowerCase.contains("(ис") || lowerCase.contains("срок)")
                || lowerCase.contains("(випр") || lowerCase.contains("терм");
    }

    private boolean hasBeenJustRemoved(String personName)
    {
        return group.getRemovedPeople().stream()
                .filter(value -> StringUtils.equalsIgnoreCase(value, personName))
                .findFirst().isPresent();
    }

    private boolean hasBeenJustAdded(String personName)
    {
        return group.getAddedPeople().stream()
                .filter(value -> StringUtils.equalsIgnoreCase(value, personName))
                .findFirst().isPresent();
    }

    private Category defineNotWhiteCategory(boolean isGreyBackGround, boolean onTrial)
    {
        Category notWhileCategory;
        if (onTrial)
        {
            notWhileCategory = Category.TRIAL;
        }
        else if (isGreyBackGround)
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
