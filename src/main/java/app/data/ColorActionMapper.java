package app.data;

import app.entities.EventType;
import app.entities.ColorWrapper;
import com.google.api.services.sheets.v4.model.Color;

import java.util.Map;

public class ColorActionMapper
{
    private Map<EventType, Color> colors;

    public ColorActionMapper(Map<EventType, Color> colors) {
        this.colors = colors;
    }

    /**
     * By cell background get kind of action (meeting, visit, call)
     * @param color cell background
     */
    public EventType getActionByColor(Color color) {
        for (Map.Entry<EventType, Color> e : colors.entrySet()) {
            if (new ColorWrapper(e.getValue()).equals(new ColorWrapper(color))) {
                return e.getKey();
            }
        }
        return null;
    }

    public Color getColorForAction(EventType eventType)
    {
        return colors.get(eventType);
    }
}
