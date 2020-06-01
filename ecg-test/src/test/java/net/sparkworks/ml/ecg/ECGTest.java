package net.sparkworks.ml.ecg;

import lombok.extern.slf4j.Slf4j;
import net.sparkworks.ml.common.util.DataUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@Slf4j
@SpringBootApplication(scanBasePackages = {"net.sparkworks.ml.ecg"})
public class ECGTest implements CommandLineRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(ECGTest.class, args);
    }
    
    @Value("${ecg.location}")
    String ecgLocation;
    
    @Override
    public void run(final String... args) {
        
        final File f = new File(ecgLocation);
        for (final File file : f.listFiles()) {
            final ECGTrace trace = new ECGTrace(file);
            log.info("=====================================================");
            log.info("File: " + trace.getFilename());
            log.info("Channels: " + trace.getNumberOfChannels());
            log.info("Records: " + trace.getNumberOfRecords());
            log.info("Duration: " + trace.getDurationOfRecords() + " records");
            log.info("Duration: " + trace.getTraceDuration() + " seconds");
            log.info("Samples: " + trace.getSamplesCount());
            final int rate = trace.getSamplesCount() / trace.getNumberOfRecords();
            log.info("Rate: " + rate);
            
            final double[] rrIntervals = trace.getRRIntervals();
            
            final SummaryStatistics statistics = DataUtils.calculateStatistics(rrIntervals);
            log.info("RR-Mean: " + statistics.getMean());
            log.info("RR-std: " + statistics.getStandardDeviation());
            
            //            for (int record = 0; record < trace.getNumberOfRecords(); record++) {
            //                System.out.print(record + ",");
            //                for (int j = 0; j < beat.length; j++) {
            //                    for (int channel = 0; channel < trace.getNumberOfChannels(); channel++) {
            //                        double[] beat = trace.getBeat(channel, record);
            //                        System.out.print(beat[j] + ",");
            //                    }
            //                }
            //                log.info("");
            //            }
        }
    }
}
