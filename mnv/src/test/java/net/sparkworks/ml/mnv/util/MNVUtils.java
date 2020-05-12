package net.sparkworks.ml.mnv.util;

import lombok.extern.slf4j.Slf4j;
import net.sparkworks.ml.mnv.model.DayData;
import net.sparkworks.ml.mnv.model.MNVData;
import net.sparkworks.ml.mnv.model.MNVDataset;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
public class MNVUtils {
    
    public static MNVData calculateFirstLevelStatistics(final MNVDataset dataset, boolean excludeBoundaryValues) {
        final SummaryStatistics statistics = new SummaryStatistics();
        final List<DayData> dayDataPicked = new ArrayList<>();
        dataset.getDayData().values().stream().filter(dayData1 -> Objects.nonNull(dayData1.getDiff())).forEach(
                dayDataPicked::add);
        if (excludeBoundaryValues) {
            Collections.sort(dayDataPicked);
            int boundaryItemsCount = (int) (dayDataPicked.size() * 0.05);
            log.debug("boundary items count: {}", boundaryItemsCount);
            for (int i = 0; i < boundaryItemsCount; i++) {
                final DayData lowItem = dayDataPicked.remove(0);
                final DayData highItem = dayDataPicked.remove(dayDataPicked.size() - 1);
                log.debug("dataRemoved: {}", lowItem);
                log.debug("dataRemoved: {}", highItem);
            }
        }
        dayDataPicked.stream().map(DayData::toString).forEach(log::debug);
        
        for (final DayData dayData : dayDataPicked) {
            if (dayData.getDiff() != null) {
                statistics.addValue(dayData.getDiff());
            }
        }
        return MNVData.builder().meanVolume(statistics.getMean()).valuesCount(statistics.getN()).std(
                statistics.getStandardDeviation()).build();
    }
    
    public static void evaluateMNVolume(final MNVData mnvData, final MNVDataset evaluationDataset) {
        evaluationDataset.getDayData().values().forEach(dayData -> evaluateMNVolume(mnvData, dayData));
    }
    
    public static boolean evaluateMNVolume(final MNVData mnvData, final DayData evaluationDataPoint) {
        double upperThreshold = mnvData.getMeanVolume() + 5 * mnvData.getStd();
        if (Objects.nonNull(evaluationDataPoint.getDiff()) && evaluationDataPoint.getDiff() > upperThreshold) {
            log.info("Value exceeding MNV threshold! [{}]  {}m3 > {}m3 [{}/m3]",
                    new Date(evaluationDataPoint.getTimestamp()), evaluationDataPoint.getDiff() / 1000,
                    mnvData.getMeanVolume() / 1000, upperThreshold / 1000);
            return true;
        }
        return false;
    }
}
