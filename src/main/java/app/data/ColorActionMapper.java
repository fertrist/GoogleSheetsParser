package app.data;

import app.entities.Action;
import app.entities.ColorWrapper;
import com.google.api.services.sheets.v4.model.Color;

import java.util.Map;

public class ColorActionMapper
{
    private Map<Action, Color> colors;

    public ColorActionMapper(Map<Action, Color> colors) {
        this.colors = colors;
    }

    /**
     * By cell background get kind of action (meeting, visit, call)
     * @param color cell background
     */
    public Action getActionByColor(Color color) {
        for (Map.Entry<Action, Color> e : colors.entrySet()) {
            if (new ColorWrapper(e.getValue()).equals(new ColorWrapper(color))) {
                return e.getKey();
            }
        }
        return null;
    }

    public Color getColorForAction(Action action)
    {
        return colors.get(action);
    }
}
