package com.mgmtp.perfload.perfalyzer.reportpreparation;

import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listPerfAlyzerFiles;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.perfAlyzerFileNameContains;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.loadProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.setIfNonNull;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.*;

/**
 * Created by amueller on 07.10.2019.
 */
public class GcLogReportPreparationStrategyTest {

    @Test
    public void testProcessFilesOldG1Format() throws Exception {
        testProcessGCFile("[gclog_old].log",10170);
    }

    @Test
    public void testProcessFilesJDK11G1Format() throws Exception {
        testProcessGCFile("[gclog_11].log",4238);
    }

    private void testProcessGCFile(String filename, int expectedLinesParsed) throws Exception {
        Locale de=Locale.GERMANY;
        NumberFormat nfint = NumberFormat.getIntegerInstance(de);
        nfint.setGroupingUsed(false);
        nfint.setRoundingMode(RoundingMode.HALF_UP);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(de);
        NumberFormat nffloat = new DecimalFormat("0.00", dfs);
        nffloat.setGroupingUsed(false);
        nffloat.setRoundingMode(RoundingMode.HALF_UP);
        String srcdir=this.getClass().getClassLoader().getResource("reportpreparation/gclogs/").getFile();
        TestMetadata meta = readTestMetadata(srcdir);
        ZonedDateTime testStartDate = meta.getTestStart();
        ZonedDateTime testEndDate = meta.getTestEnd();
        TimestampNormalizer tsnormalizer = new TimestampNormalizer(testStartDate, testEndDate, 5);
        GcLogReportPreparationStrategy strategy = new GcLogReportPreparationStrategy(
                nfint, nffloat, new ArrayList<DisplayData>(), null,
                null, meta, tsnormalizer, null, null, null);
        PerfAlyzerFile gcFile = PerfAlyzerFile.create(new File(filename));
        List<PerfAlyzerFile> files = new ArrayList<PerfAlyzerFile>(1);
        files.add(gcFile);
        strategy.processFiles(new File(srcdir), new File("c:\temp"), files);
        Assert.assertEquals(strategy.numberOfLinesParsed,expectedLinesParsed);
    }

    private TestMetadata readTestMetadata(String srcdir) {
        File metaPropsFile = new File(srcdir, "perfload.meta.utf8.props");

        Properties perfLoadMetaProps;
        if (metaPropsFile.exists()) {
            try {
                perfLoadMetaProps = loadProperties(metaPropsFile);
            } catch (IOException ioexc) {
                ioexc.printStackTrace();
                perfLoadMetaProps = new Properties();
            }
        } else {
            perfLoadMetaProps = new Properties();
        }
        return TestMetadata.create(srcdir, perfLoadMetaProps);
    }

}