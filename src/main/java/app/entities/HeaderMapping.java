package app.entities;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static app.entities.WebConfigurationColumn.findColumnByString;

public class HeaderMapping
{
    Map<WebConfigurationColumn, Integer> mapping;

    private HeaderMapping()
    {
        mapping = new HashMap<>();
    }

    public Integer getIndexFor(WebConfigurationColumn column)
    {
        return mapping.get(column);
    }

    public static HeaderMapping buildFrom(RowData headerConfigurationRow)
    {
        HeaderMapping headerMapping = new HeaderMapping();
        headerMapping.buildFromRow(headerConfigurationRow);
        return headerMapping;
    }

    private void buildFromRow(RowData headerConfigurationRow)
    {
        List<CellData> headerRow = headerConfigurationRow.getValues();
        for (int i = 0; i < headerRow.size(); i++)
        {
            CellData cellData = headerRow.get(i);
            String value = cellData.getEffectiveValue().getStringValue();
            if (StringUtils.isBlank(value))
            {
                break;
            }
            final int a = i;
            findColumnByString(value).ifPresent(column -> mapping.put(column, a));
        }
    }
}
