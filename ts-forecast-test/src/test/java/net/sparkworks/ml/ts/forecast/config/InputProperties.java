package net.sparkworks.ml.ts.forecast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("input")
public class InputProperties {
    
    private List<String> uuids;
    
    public List<String> getUuids() {
        return uuids;
    }
    
    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }
}
