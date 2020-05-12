package net.sparkworks.ml.mnv.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.cargo.common.dto.data.ResourceDataDTO;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@Builder
public class MNVDataset {
    
    private static int MNV_START_HOUR = 2;
    private static int MNV_END_HOUR = MNV_START_HOUR + 3;
    
    private ResourceDTO resource;
    private final Map<Integer, DayData> dayData = new HashMap<>();
    
    public void offer(final Collection<ResourceDataDTO> data) {
        data.forEach(this::offer);
    }
    
    public void offer(final ResourceDataDTO datum) {
        final Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(datum.getTimestamp());
        final int day = cal1.get(Calendar.DAY_OF_YEAR);
        final int hour = cal1.get(Calendar.HOUR_OF_DAY);
        if (!dayData.containsKey(day)) {
            dayData.put(day, DayData.builder().day(day).timestamp(datum.getTimestamp()).build());
        }
        if (hour == MNV_START_HOUR) {
            dayData.get(day).setValueStart(datum.getReading());
        } else if (hour == MNV_END_HOUR) {
            dayData.get(day).setValueEnd(datum.getReading());
        }
    }
    
    public void add(final DayData dayPoint) {
        dayData.put(dayPoint.getDay(), dayPoint);
    }
    
}
