package net.sparkworks.ml.mnv;


import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.client.GroupClient;
import net.sparkworks.cargo.client.ResourceClient;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.ml.mnv.config.MnvInputProperties;
import net.sparkworks.ml.mnv.model.MNVData;
import net.sparkworks.ml.mnv.model.MNVDataset;
import net.sparkworks.ml.mnv.service.DatasetService;
import net.sparkworks.ml.mnv.util.MNVUtils;
import net.sparkworks.ml.mnv.util.ResourceUtils;
import net.sparkworks.rs.client.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;

import static net.sparkworks.cargo.client.config.CargoClientConfig.CARGO_CLIENT_BASE_PACKAGE_NAME;
import static net.sparkworks.rs.client.impl.config.RegistryClientConfig.REGISTRY_CLIENT_BASE_PACKAGE_NAME;

@Slf4j
@SpringBootApplication(scanBasePackages = {"net.sparkworks.ml.mnv", CARGO_CLIENT_BASE_PACKAGE_NAME, REGISTRY_CLIENT_BASE_PACKAGE_NAME})
public class MNVolumeTest implements CommandLineRunner {
    
    @Autowired
    private MnvInputProperties inputProperties;
    
    public static void main(String[] args) {
        SpringApplication.run(MNVolumeTest.class, args);
    }
    
    @Autowired
    ResourceClient resourceClient;
    @Autowired
    GroupClient groupClient;
    @Autowired
    DatasetService datasetService;
    
    @Autowired
    RegistryClient registryClient;
    
    @Override
    public void run(final String... args) {
        
        final UUID baseGroupUuid = UUID.fromString(inputProperties.getGroup());
        final UUID wVolumePhenomenonUuid = UUID.fromString(inputProperties.getPhenomenon());
        
        final Calendar cal = Calendar.getInstance();
        long timeNow = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -2 * 30);
        long time1 = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -2 * 30);
        long time2 = cal.getTimeInMillis();
        log.debug("timeNow:{} time1:{} time2:{}", timeNow, time1, time2);
        
        final Collection<ResourceDTO> resources = groupClient.getGroupResources(baseGroupUuid);
        for (final ResourceDTO resource : resources) {
            if (resource.getPhenomenonUuid() != null && resource.getPhenomenonUuid().equals(wVolumePhenomenonUuid)) {
                log.info("=====================================");
                log.info("Resource: {}", resource.getSystemName());
                log.debug("SystemName: {}", resource.getSystemName());
                log.debug("Name: {}", resource.getUserFriendlyName());
                
                String building = ResourceUtils.getBuilding(registryClient, resource);
                log.info("Building {}", building);
                
                //get training dataset
                final MNVDataset mnvDataset = datasetService.getDataset(resource, time2, time1);
                //get mnv data for the training dataset
                final MNVData mnv = MNVUtils.calculateFirstLevelStatistics(mnvDataset);
                log.info("MeanNightVolume: {}m3 samples: {} std: {}m3", mnv.getMeanVolume() / 1000,
                        mnv.getValuesCount(), mnv.getStd() / 1000);
                //get test dataset
                final MNVDataset mnvDataset2 = datasetService.getDataset(resource, time1, timeNow);
                //evaluate test dataset
                MNVUtils.evaluateMNVolume(mnv, mnvDataset2);
                
            }
        }
    }
    
}
