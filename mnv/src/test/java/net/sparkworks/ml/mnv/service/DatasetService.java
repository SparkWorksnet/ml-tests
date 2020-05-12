package net.sparkworks.ml.mnv.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.client.DataClient;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.cargo.common.dto.data.AnalyticsResourceDataResponseDTO;
import net.sparkworks.cargo.common.dto.data.Granularity;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataCriteriaDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataResultDTO;
import net.sparkworks.ml.mnv.model.DayData;
import net.sparkworks.ml.mnv.model.MNVDataset;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {
    
    private final DataClient dataClient;
    
    public MNVDataset getDataset(final ResourceDTO resource, final long from, final long to) {
        final QueryTimeRangeResourceDataCriteriaDTO criterion = new QueryTimeRangeResourceDataCriteriaDTO();
        criterion.setTo(to);
        criterion.setFrom(from);
        criterion.setGranularity(Granularity.HOUR);
        criterion.setResourceUuid(resource.getUuid());
        log.debug("Retrieving data {}", criterion);
        final QueryTimeRangeResourceDataResultDTO response = dataClient.queryTimeRangeResourcesData(
                QueryTimeRangeResourceDataDTO.builder().queries(Collections.singletonList(criterion)).build());
        final AnalyticsResourceDataResponseDTO value = response.getResults().values().iterator().next();
        final MNVDataset dataset = MNVDataset.builder().resource(resource).build();
        value.getData().forEach(dataset::offer);
        return dataset;
    }
    
    public MNVDataset getDataset(final ResourceDTO resource, final Deque<DayData> queue) {
        final MNVDataset dataset = MNVDataset.builder().resource(resource).build();
        queue.forEach(dataset::add);
        return dataset;
    }
    
    public MNVDataset merge(final MNVDataset mnvDatasetSmall, final MNVDataset mnvDatasetLarge) {
        final MNVDataset dataset = MNVDataset.builder().build();
        dataset.getDayData().putAll(mnvDatasetSmall.getDayData());
        for (final DayData value : mnvDatasetLarge.getDayData().values()) {
            if (dataset.getDayData().containsKey(value.getDay())) {
                merge(dataset.getDayData().get(value.getDay()), value);
            } else {
                dataset.getDayData().put(value.getDay(), value);
            }
        }
        return dataset;
    }
    
    private void merge(final DayData dayData, final DayData value) {
        if (dayData.getDiff() != null && value.getDiff() != null) {
            dayData.setDiff(dayData.getDiff() + value.getDiff());
        } else if (value.getDiff() != null) {
            dayData.setDiff(value.getDiff());
        }
    }
}
