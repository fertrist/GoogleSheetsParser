package app;

import app.dao.SheetApi;
import app.entities.HeaderMapping;
import app.entities.Row;
import app.entities.WebConfigurationColumn;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;


import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pass CONFIGURATION_FILE as vm option for example:
 * -DCONFIGURATION_FILE=/home/myUser/.../resources/report.configuration.properties
 */
@Configuration
@ComponentScan(basePackages = "app")
public class Application {

    private static String CONFIG_URL = "1c3larbSWfcSW6Z23t_8TqUHDS3XEwLFk-C5N9665yTM";

    @Autowired
    private SheetApi sheetApi;

    public static void main(String[] args) throws IOException
    {
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(Application.class);
        context.getBean(Application.class).start();
        context.close();
    }

    private void start() throws IOException
    {
        getWebConfiguration(sheetApi);

        /*GroupSheetApi.setSheetApi(sheetApi);

        ReportGenerator reportGenerator = new ReportGenerator();
        List<RegionReport> reports = reportGenerator.collectRegionReports();

        ReportPrinter.printReports(sheetsService, reports);*/
    }

    static void getWebConfiguration(SheetApi sheetApi) throws IOException
    {
        Sheet sheet = sheetApi.getSheet(CONFIG_URL, 1, 25);
        List<RowData> rows = sheet.getData().get(0).getRowData();
        System.out.println(rows.size());
    }

    private void breakRowsIntoConfiguration(List<RowData> rows)
    {
        HeaderMapping headerMapping = HeaderMapping.buildFrom(rows.get(0));

        for (int i = 1; i < rows.size(); i++)
        {
            List<CellData> row = rows.get(i).getValues();
            int index = row.get(headerMapping.getIndexFor(WebConfigurationColumn.INCLUDE));
        }
        // define columns for all configurations

        // use that columns to get values from each row
    }

    private List<Row> toRows(List<RowData> rowDataList)
    {
        return rowDataList.stream().map(Row::new).collect(Collectors.toList());
    }
}
