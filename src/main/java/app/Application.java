package app;

import app.conf.SheetsApp;
import app.dao.SheetApi;
import app.dao.GroupSheetApi;
import app.extract.ReportPrinter;
import app.generate.ReportGenerator;
import app.report.RegionReport;
import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.util.List;

/**
 * Pass CONFIGURATION_FILE as vm option for example:
 * -DCONFIGURATION_FILE=/home/myUser/.../resources/report.configuration.properties
 */
public class Application {

    public static void main(String[] args) throws IOException
    {
        Sheets sheetsService = SheetsApp.getSheetsService();
        SheetApi sheetApi = new SheetApi(sheetsService);
        GroupSheetApi.setSheetApi(sheetApi);

        ReportGenerator reportGenerator = new ReportGenerator();
        List<RegionReport> reports = reportGenerator.collectRegionReports();

        ReportPrinter.printReports(sheetsService, reports);
    }

}
