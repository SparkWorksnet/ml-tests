package net.sparkworks.ml.mnv.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
public class MNVData {
    
    private double meanVolume;
    private long valuesCount;
    private double std;
    
}
