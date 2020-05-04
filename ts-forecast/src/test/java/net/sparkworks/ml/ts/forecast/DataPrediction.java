package net.sparkworks.ml.ts.forecast;


import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.client.DataClient;
import net.sparkworks.cargo.client.ResourceClient;
import net.sparkworks.cargo.client.config.CargoClientConfig;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.cargo.common.dto.data.Granularity;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataCriteriaDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataResultDTO;
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
    
    //        private static final UUID RESOURCE_UUID = UUID.fromString("b5442ed6-532e-40df-a948-1f8d72af5f22"); //pressure auth1
    //        private static final UUID RESOURCE_UUID = UUID.fromString("34a8e7c7-938d-4497-9a7f-6d93cce33253"); //ambientTemp auth1
    //        private static final UUID RESOURCE_UUID = UUID.fromString("38d69823-9ab5-4592-9b1d-09c1ee08606b"); //ambientTemp auth2
    //        private static final UUID RESOURCE_UUID = UUID.fromString("6bd12dc8-a22a-44ce-b8f4-a0b46b0909e6"); //ambientTemp auth3
    //        private static final UUID RESOURCE_UUID = UUID.fromString("f93e3f27-aca8-4bc2-9406-da855a2bc060"); //waterVolume authx
    private static final UUID RESOURCE_UUID = UUID.fromString("3d10525c-8a57-4e27-97c8-50702b746b42"); //waterVolume authx
    //        private static final UUID RESOURCE_UUID = UUID.fromString("3da34fb1-e6fd-4740-86d1-9853796506f9");
    //    private static final UUID RESOURCE_UUID = UUID.fromString("a1bb55bb-a9ed-44c0-ac0c-b5e16fa52b36");
    //    private static final UUID RESOURCE_UUID = UUID.fromString("dfaa771d-796e-4243-abcd-0692b2fe3559");
    //    private static final UUID RESOURCE_UUID = UUID.fromString("0ad7138c-22ec-4552-b7aa-35b0677f796d");
    //    private static final UUID RESOURCE_UUID = UUID.fromString("6f8f6f48-18e8-4a0d-996b-ae7e5040f2dc");
    
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
        criterion.setResourceUuid(RESOURCE_UUID);
        log.info("Retrieving data {}", criterion);
        final QueryTimeRangeResourceDataResultDTO response1 = dataClient.queryTimeRangeResourcesData(QueryTimeRangeResourceDataDTO.builder().queries(Collections.singletonList(criterion)).build());
        
        final long start = System.currentTimeMillis();
        
        final double[] values = DataUtils.queryResponse2DataArray(response1.getResults().entrySet().iterator().next().getValue().getData());
        log.info("Retrieved data size: {}", values.length);
        
        
        final double[] predictionData = ArrayUtils.subarray(values, 0, values.length - FORECAST_SIZE);
        final double[] dataToBePredicted = ArrayUtils.subarray(values, values.length - FORECAST_SIZE, values.length);
        
        
        // Set ARIMA model parameters.
        //model for data every 5 minutes
        //final ArimaParams params = new ArimaParams(12, 0, 10, 24, 0, 10, 7);
        //model for data every 1 hour
        final ArimaParams params = new ArimaParams(24, 0, 2, 35, 0, 2, 7);
        
        // Obtain forecast result. The structure contains forecasted values and performance metric etc.
        final ForecastResult forecastResult = Arima.forecast_arima(predictionData, FORECAST_SIZE, params);
        // Read forecast values
        final double[] forecastData = forecastResult.getForecast();
        
        //cleanup negative values
        for (int j = 0; j < FORECAST_SIZE; j++) {
            if (forecastData[j] < 0) {
                forecastData[j] = 0;
            }
        }
        
        testSmoothWithDifferentParams(predictionData, forecastData, dataToBePredicted);
        
        final double[] smoothedForecastData = DataUtils.combineAndSmoothen(predictionData, forecastData, 1.0, 0.1);
        
        //cleanup negative values
        for (int i = 0; i < FORECAST_SIZE; i++) {
            if (smoothedForecastData[i] < 0) {
                smoothedForecastData[i] = 0;
            }
        }
        
        final long diff = System.currentTimeMillis() - start;
        
        System.out.print("forecast,forecast-smoothed,value");
        //print data
        for (int i = 0; i < forecastData.length; i++) {
            System.out.print(forecastData[i] + ",");
            System.out.print(smoothedForecastData[i] + ",");
            System.out.println(dataToBePredicted[i]);
        }
        
        return diff;
    }
    
    private static void testSmoothWithDifferentParams(final double[] predictionData, final double[] forecastData, final double[] dataToBePredicted) {
        log.info(String.format("Forecast Sum of squared error %15.0f", DataUtils.calculateSSE(dataToBePredicted, forecastData)));
        for (double alpha = 0.1; alpha <= 1.0; alpha += 0.1) {
            for (double beta = 0.1; beta <= 1.0; beta += 0.1) {
                final double[] smoothedForecastData = DataUtils.combineAndSmoothen(predictionData, forecastData, alpha, beta);
                for (int i = 0; i < FORECAST_SIZE; i++) {
                    if (smoothedForecastData[i] < 0) {
                        smoothedForecastData[i] = 0;
                    }
                }
                log.info(String.format("Smoothed Sum of squared error [%.1f|%.1f] %15.0f", alpha, beta, DataUtils.calculateSSE(dataToBePredicted, smoothedForecastData)));
            }
        }
    }
    
}
