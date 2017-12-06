package app;

import app.dao.CustomSheetApi;
import app.dao.GroupSheetApi;
import app.report.RegionReport;
import app.conf.SheetsApp;
import app.generate.ReportGenerator;
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
        CustomSheetApi sheetApi = new CustomSheetApi(sheetsService);
        GroupSheetApi.setSheetApi(sheetApi);

        // TODO collect metadata into wrapper

        ReportGenerator reportGenerator = new ReportGenerator();
        List<RegionReport> regionReportReports = reportGenerator.collectRegionReports();
    }

}