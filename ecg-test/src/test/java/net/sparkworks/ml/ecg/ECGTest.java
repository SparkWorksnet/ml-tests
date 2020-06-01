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
            System.out.println("=====================================================");
            System.out.println("File: " + trace.getFilename());
            System.out.println("Channels: " + trace.getNumberOfChannels());
            System.out.println("Records: " + trace.getNumberOfRecords());
            System.out.println("Duration: " + trace.getDurationOfRecords() + " records");
            System.out.println("Duration: " + trace.getTraceDuration() + " seconds");
            System.out.println("Samples: " + trace.getSamplesCount());
            final int rate = trace.getSamplesCount() / trace.getNumberOfRecords();
            System.out.println("Rate: " + rate);
            
            final double[] rrIntervals = trace.getRRIntervals();
            
            final SummaryStatistics statistics = DataUtils.calculateStatistics(rrIntervals);
            System.out.println("RR-Mean: " + statistics.getMean());
            System.out.println("RR-std: " + statistics.getStandardDeviation());
            
            //            for (int record = 0; record < trace.getNumberOfRecords(); record++) {
            //                System.out.print(record + ",");
            //                for (int j = 0; j < beat.length; j++) {
            //                    for (int channel = 0; channel < trace.getNumberOfChannels(); channel++) {
            //                        double[] beat = trace.getBeat(channel, record);
            //                        System.out.print(beat[j] + ",");
            //                    }
            //                }
            //                System.out.println("");
            //            }
        }
    }
}
