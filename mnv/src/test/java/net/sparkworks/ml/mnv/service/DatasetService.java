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
import net.sparkworks.ml.mnv.model.MNVDataset;
import org.springframework.stereotype.Service;

import java.util.Collections;

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
}
