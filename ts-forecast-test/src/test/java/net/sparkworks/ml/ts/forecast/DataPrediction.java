package net.sparkworks.ml.ts.forecast;


import com.workday.insights.timeseries.arima.struct.ArimaParams;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.client.DataClient;
import net.sparkworks.cargo.client.ResourceClient;
import net.sparkworks.cargo.client.config.CargoClientConfig;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.cargo.common.dto.data.Granularity;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataCriteriaDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataResultDTO;
import net.sparkworks.ml.common.util.DataUtils;
import net.sparkworks.ml.ts.forecast.config.InputProperties;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@SpringBootApplication(scanBasePackages = {"net.sparkworks.ml.ts.forecast", CargoClientConfig.CARGO_CLIENT_BASE_PACKAGE_NAME})
public class DataPrediction implements CommandLineRunner {
    @Autowired
    private InputProperties inputProperties;
    
    private static final int FORECAST_SIZE = 2 * 7 * 24;
    
    public static void main(String[] args) {
        SpringApplication.run(DataPrediction.class, args);
    }
    
    @Autowired
    ResourceClient resourceClient;
    
    @Autowired
    DataClient dataClient;
    
    @Override
    public void run(final String... args) {
        int totalTime = 0;
        for (final String uuid : inputProperties.getUuids()) {
            totalTime += predictData(uuid);
        }
        log.info("took {}ms on average", (totalTime / inputProperties.getUuids().size()));
    }
    
    private long predictData(final String uuid) {
        final ResourceDTO resource = resourceClient.getByUUID(UUID.fromString(uuid));
        log.info("Resource: {}", resource.getUuid());
        log.info("SystemName: {}", resource.getSystemName());
        log.info("Name: {}", resource.getUserFriendlyName());
        
        final QueryTimeRangeResourceDataCriteriaDTO criterion = new QueryTimeRangeResourceDataCriteriaDTO();
        //        criterion.setFrom(1585702800000L);
        //        criterion.setTo(1588234614000L);
        criterion.setFrom(1578704400000L);
        criterion.setTo(1582938000000L);
        criterion.setGranularity(Granularity.HOUR);
        criterion.setResourceUuid(UUID.fromString(uuid));
        log.info("Retrieving data {}", criterion);
        final QueryTimeRangeResourceDataResultDTO response1 = dataClient.queryTimeRangeResourcesData(
                QueryTimeRangeResourceDataDTO.builder().queries(Collections.singletonList(criterion)).build());
        
        final double[] values = DataUtils.queryResponse2DataArray(
                response1.getResults().entrySet().iterator().next().getValue().getData());
        log.info("Retrieved data size: {}", values.length);
        
        
        final double[] predictionData = ArrayUtils.subarray(values, 0, values.length - FORECAST_SIZE);
        final double[] dataToBePredicted = ArrayUtils.subarray(values, values.length - FORECAST_SIZE, values.length);
        
        testSmoothWithDifferentParams(predictionData, dataToBePredicted);
        
        final long start = System.currentTimeMillis();
        final double[] smoothedForecastData = DataPredictor.predictHourlyData(predictionData, FORECAST_SIZE);
        return System.currentTimeMillis() - start;
    }
    
    private static void testSmoothWithDifferentParams(final double[] predictionData, final double[] dataToBePredicted) {
        final ArimaParams arimaParams = new ArimaParams(24, 0, 2, 35, 0, 2, 7);
        for (double alpha = 0.1; alpha <= 1.0; alpha += 0.1) {
            for (double beta = 0.1; beta <= 1.0; beta += 0.1) {
                final double[] smoothedForecastData = DataPredictor.predictHourlyData(predictionData, FORECAST_SIZE,
                        true, arimaParams, new SmootingParameters(alpha, beta));
                for (int i = 0; i < FORECAST_SIZE; i++) {
                    if (smoothedForecastData[i] < 0) {
                        smoothedForecastData[i] = 0;
                    }
                }
                //log.info(String.format("Smoothed Sum of squared error [%.1f|%.1f] %15.0f", alpha, beta, DataUtils.calculateSSE(dataToBePredicted, smoothedForecastData)));
                log.info(String.format("Smoothed RMSPE of smoothedForecasted Data [%.1f|%.1f] %5.5f", alpha, beta,
                        DataUtils.calculateMPE(dataToBePredicted, smoothedForecastData)));
            }
        }
    }
    
}
