package net.sparkworks.ml.mnv.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("mnv")
public class MnvInputProperties {
    
    private String group;
    private String phenomenon;
    
}
