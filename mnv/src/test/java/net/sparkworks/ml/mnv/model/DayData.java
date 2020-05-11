package net.sparkworks.ml.mnv.model;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder
public class DayData implements Comparable<DayData> {
    private int day;
    private long timestamp;
    private Double valueStart;
    private Double valueEnd;
    private Double diff;
    
    public void setValueStart(Double valueStart) {
        this.valueStart = valueStart;
        updatMNVolume();
    }
    
    public void setValueEnd(Double valueEnd) {
        this.valueEnd = valueEnd;
        updatMNVolume();
    }
    
    private void updatMNVolume() {
        if (Objects.nonNull(valueStart) && Objects.nonNull(valueEnd) && valueStart > 0 && valueEnd > 0) {
            diff = valueEnd - valueStart;
        } else {
            diff = null;
        }
    }
    
    @Override
    public int compareTo(DayData o) {
        return diff.compareTo(o.getDiff());
    }
}
