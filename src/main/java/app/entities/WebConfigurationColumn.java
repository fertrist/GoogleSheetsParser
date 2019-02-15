package app.entities;

import java.util.Optional;

public enum WebConfigurationColumn {
    INCLUDE("ВКЛЮЧИТЬ"),
    REGION("РЕГИОН"),
    LEADER("ЛИДЕР"),
    ADDED("ДОБАВЛЕНЫ"),
    REMOVED("УДАЛЕНЫ"),
    DAY_OF_WEEK("ДЕНЬ_ПРОВЕДЕНИЯ"),
    PEOPLE_COLUMN("КОЛОНКА_СПИСКА"),
    STEPS("ЭТАПЫ"),
    TABLE("URL");

    private String column;

    WebConfigurationColumn(String column) {
        this.column = column;
    }

    public static Optional<WebConfigurationColumn> findColumnByString(String s)
    {
        for (WebConfigurationColumn column : values())
        {
            if (column.column.equalsIgnoreCase(s))
            {
                return Optional.of(column);
            }
        }
        return Optional.empty();
    }
}
