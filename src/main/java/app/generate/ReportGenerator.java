package app.generate;

import static app.conf.Configuration.GROUPS;
import static app.conf.Configuration.LEADER;
import static app.conf.Configuration.REGIONS;
import static app.conf.Configuration.getProperty;
import static app.conf.Configuration.getRegionProperty;
import app.report.GroupReport;
import app.report.RegionReport;
import app.conf.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    public List<RegionReport> collectRegionReports() throws IOException {

        return getRegionOrdinalsStream().map(this::collectRegionReport).collect(Collectors.toList());
    }

    private Stream<String> getRegionOrdinalsStream()
    {
        String[] regionNumbers = getProperty(REGIONS).split(",");
        return Arrays.stream(regionNumbers);
    }

    private RegionReport collectRegionReport(String regionOrdinalNumber)
    {
        RegionReport regionReport = createBlankRegion(regionOrdinalNumber);

        System.out.println("Processing " + regionReport.getLeader() + "'s regionReport.");

        List<GroupReport> groupReports = getGroupOrdinalsStream(regionOrdinalNumber)
                .map(Configuration::buildGroup)
                .map(GroupReportGenerator::new)
                .map(GroupReportGenerator::generateGroupReport)
                .collect(Collectors.toList());

        regionReport.setGroupReports(groupReports);

        return regionReport;
    }

    private RegionReport createBlankRegion(String regionOrdinalNumber)
    {
        String regionLeader = getRegionalLeader(regionOrdinalNumber);
        return new RegionReport(regionLeader);
    }

    private String getRegionalLeader(String regionOrdinalNumber)
    {
        return getRegionProperty(LEADER, regionOrdinalNumber);
    }

    private Stream<Integer> getGroupOrdinalsStream(String regionOrdinalNumber)
    {
        String [] groupNumbers = getRegionProperty(GROUPS, regionOrdinalNumber).split(",");
        return Arrays.stream(groupNumbers).map(Integer::valueOf);
    }

}
