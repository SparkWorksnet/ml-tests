package net.sparkworks.ml.mnv.util;

import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.rs.client.RegistryClient;
import net.sparkworks.rs.common.dto.PairDTO;
import net.sparkworks.rs.common.dto.RecordDTO;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ResourceUtils {
    
    public static String getBuilding(final RegistryClient registryClient, final ResourceDTO resource) {
        final String[] parts = resource.getSystemName().split("/");
        final String deviceName = parts[0] + "/" + parts[1];
        final List<RecordDTO> deviceNameRecords = registryClient.query(Collections.singletonList(deviceName));
        
        for (final RecordDTO record : deviceNameRecords) {
            for (final PairDTO pair : record.getPairs()) {
                if ("building".equals(pair.getKey())) {
                    return pair.getValue();
                }
            }
        }
        return null;
    }
    
    public static String getVolume(final RegistryClient registryClient, final ResourceDTO resource) {
        final String[] parts = resource.getSystemName().split("/");
        final String deviceName = parts[0] + "/" + parts[1];
        final List<RecordDTO> deviceNameRecords = registryClient.query(Collections.singletonList(deviceName));
        
        for (final RecordDTO record : deviceNameRecords) {
            for (final PairDTO pair : record.getPairs()) {
                if ("volume".equals(pair.getKey())) {
                    return pair.getValue();
                }
            }
        }
        return null;
    }
}
