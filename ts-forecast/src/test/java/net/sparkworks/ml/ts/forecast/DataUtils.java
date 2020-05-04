package net.sparkworks.ml.ts.forecast;

import net.sparkworks.cargo.common.dto.data.ResourceDataDTO;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class DataUtils {
    
    /**
     * Performs double exponential smoothing for given time series and forecasted data.
     * <p/>
     * This method is suitable for fitting series with linear trend.
     *
     * @param data  An array containing the recorded data of the time series
     * @param forecast  An array containing the generated forecast data for the time series
     * @param alpha Smoothing factor for data (0 < alpha < 1)
     * @param beta  Smoothing factor for trend (0 < beta < 1)
     * @return Instance of model that can be used to forecast future values
     */
    public static double[] combineAndSmoothen(double[] data, double[] forecast, double alpha, double beta) {
        return ArrayUtils.subarray(smoothen(ArrayUtils.addAll(data, forecast), alpha, beta), data.length - forecast.length, data.length + forecast.length);
    }
    
    /**
     * Performs double exponential smoothing for given time series.
     * <p/>
     * This method is suitable for fitting series with linear trend.
     *
     * @param data  An array containing the recorded data of the time series
     * @param alpha Smoothing factor for data (0 < alpha < 1)
     * @param beta  Smoothing factor for trend (0 < beta < 1)
     * @return Instance of model that can be used to forecast future values
     */
    public static double[] smoothen(double[] data, double alpha, double beta) {
        validateParams(alpha, beta);                        //validating values of alpha and beta
        
        double[] smoothedData = new double[data.length];    //array to store smoothed values
        
        double[] trends = new double[data.length + 1];
        double[] levels = new double[data.length + 1];
        
        //initializing values of parameters
        smoothedData[0] = data[0];
        trends[0] = data[1] - data[0];
        levels[0] = data[0];
        
        for (int t = 0; t < data.length; t++) {
            smoothedData[t] = trends[t] + levels[t];
            levels[t + 1] = alpha * data[t] + (1 - alpha) * (levels[t] + trends[t]);
            trends[t + 1] = beta * (levels[t + 1] - levels[t]) + (1 - beta) * trends[t];
        }
        return smoothedData;
    }
    
    public static double calculateSSE(double[] data, double[] forecastData) {
        double sse = 0;
        for (int i = 0; i < data.length; i++) {
            sse += Math.pow(forecastData[i] - data[i], 2);
        }
        return sse;
    }
    
    private static void validateParams(final double alpha, final double beta) {
        if (alpha < 0 || alpha > 1) {
            throw new RuntimeException("The value of alpha must be between 0 and 1");
        }
        
        if (beta < 0 || beta > 1) {
            throw new RuntimeException("The value of beta must be between 0 and 1");
        }
    }
    
    public static double[] queryResponse2DataArray(final List<ResourceDataDTO> data) {
        return data.stream().map(ResourceDataDTO::getReading).mapToDouble(x -> x).toArray();
    }
}
