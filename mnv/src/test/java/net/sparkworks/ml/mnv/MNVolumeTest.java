package net.sparkworks.ml.mnv;


import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.client.GroupClient;
import net.sparkworks.cargo.client.ResourceClient;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.ml.mnv.config.MnvInputProperties;
import net.sparkworks.ml.mnv.model.Building;
import net.sparkworks.ml.mnv.model.DayData;
import net.sparkworks.ml.mnv.model.MNVData;
import net.sparkworks.ml.mnv.model.MNVDataset;
import net.sparkworks.ml.mnv.service.DatasetService;
import net.sparkworks.ml.mnv.util.MNVUtils;
import net.sparkworks.ml.mnv.util.ResourceUtils;
import net.sparkworks.rs.client.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    
    @Value("${mnv.mode:default}")
    String mode;
    
    @Autowired
    ResourceClient resourceClient;
    @Autowired
    GroupClient groupClient;
    @Autowired
    DatasetService datasetService;
    
    @Autowired
    RegistryClient registryClient;
    
    Map<String, Building> buildingMap = new HashMap<>();
    
    @Override
    public void run(final String... args) {
        log.info("mode {}", mode);
        switch (mode) {
            case "auth":
                try {
                    runAuthTest();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
                break;
            default:
                runTest();
                break;
        }
    }
    
    private void runAuthTest() throws IOException {
        final UUID baseGroupUuid = UUID.fromString(inputProperties.getGroup());
        final UUID wVolumePhenomenonUuid = UUID.fromString(inputProperties.getPhenomenon());
        
        final Calendar cal = Calendar.getInstance();
        long timeNow = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -8 * 30);
        long time1 = cal.getTimeInMillis();
        log.debug("timeNow:{} time1:{}", timeNow, time1);
        
        File base = new File("./results/");
        if (!base.exists()) {
            base.mkdir();
        }
        
        final Collection<ResourceDTO> resources = groupClient.getGroupResources(baseGroupUuid);
        for (final ResourceDTO resource : resources) {
            if (resource.getPhenomenonUuid() != null && resource.getPhenomenonUuid().equals(
                    wVolumePhenomenonUuid) && resource.getSystemName().startsWith("mblr-2")) {
                log.info("processing {}", resource.getSystemName());
                final String building = ResourceUtils.getBuilding(registryClient, resource);
                final String volume = ResourceUtils.getVolume(registryClient, resource);
                if (!buildingMap.containsKey(building)) {
                    buildingMap.put(building, Building.builder().name(building).build());
                }
                if ("1".equals(volume)) {
                    buildingMap.get(building).setSmall(resource);
                }
                if (volume != null) {
                    buildingMap.get(building).setLarge(resource);
                }
            }
        }
        
        int count = 0;
        
        for (final Building building : buildingMap.values()) {
            log.info("=====================================");
            log.info("Building {}", building.getName());
            log.info("Small: {}", building.getSmall() != null ? building.getSmall().getSystemName() : "");
            log.info("Large: {}", building.getLarge() != null ? building.getLarge().getSystemName() : "");
            if (count < 12) {
                count++;
                continue;
            }
            
            //get training dataset
            MNVDataset mnvDatasetSmall = MNVDataset.builder().build();
            MNVDataset mnvDatasetLarge = MNVDataset.builder().build();
            
            if (building.getSmall() != null) {
                mnvDatasetSmall = datasetService.getDataset(building.getSmall(), time1, timeNow);
            }
            if (building.getLarge() != null) {
                mnvDatasetLarge = datasetService.getDataset(building.getLarge(), time1, timeNow);
            }
            
            MNVDataset mnvDataset = datasetService.merge(mnvDatasetSmall, mnvDatasetLarge);
            
            final List<DayData> values = new ArrayList(mnvDataset.getDayData().values());
            values.sort(Comparator.comparing(DayData::getTimestamp));
            
            
            final Deque<DayData> queue = new ArrayDeque<>();
            
            final File f = new File(base, count++ + "-" + building.getName().replaceAll("/", "") + ".csv");
            f.delete();
            f.createNewFile();
            final FileWriter fw = new FileWriter(f);
            fw.write("time\tmnv\tsamples\tstd\tlimit\tnv\tisAbnormal\n");
            Iterator<DayData> it = values.iterator();
            
            DayData value = it.next();
            do {
                if (value != null) {
                    log.debug("{} - {}", new Date(value.getTimestamp()), value.getDiff());
                    if (value.getDiff() != null) {//&& value.getDiff() > 0
                        queue.addLast(value);
                    }
                    if (queue.size() > 15) {
                        queue.removeFirst();
                    }
                }
                if (queue.size() == 15) {
                    final MNVDataset dataset = datasetService.getDataset(building.getSmall(), queue);
                    final MNVData mnv = MNVUtils.calculateFirstLevelStatistics(dataset, false);
                    if (it.hasNext()) {
                        final DayData nextValue = it.next();
                        if (nextValue.getDiff() != null) {
                            final boolean isAbnormal = MNVUtils.evaluateMNVolume(mnv, nextValue);
                            fw.write(String.format(Locale.US, "%s\t%.4f\t%d\t%.4f\t%.4f\t%.4f\t%d\n",
                                    new Date(nextValue.getTimestamp()), mnv.getMeanVolume() / 1000,
                                    mnv.getValuesCount(), mnv.getStd() / 1000,
                                    mnv.getMeanVolume() / 1000 + 5 * mnv.getStd() / 1000, nextValue.getDiff() / 1000,
                                    isAbnormal ? 1 : 0));
                            if (isAbnormal) {
                                value = null;
                            } else {
                                value = nextValue;
                            }
                        } else {
                            fw.write(String.format(Locale.US, "%s\t%.4f\t%d\t%.4f\t%.4f\t\t\n",
                                    new Date(nextValue.getTimestamp()), mnv.getMeanVolume() / 1000,
                                    mnv.getValuesCount(), mnv.getStd() / 1000,
                                    mnv.getMeanVolume() / 1000 + 5 * mnv.getStd() / 1000, 0));
                            value = nextValue;
                        }
                    }
                } else {
                    value = it.next();
                }
            } while (it.hasNext());
            fw.close();
        }
    }
    
    private void runTest() {
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
            if (resource.getPhenomenonUuid() != null && resource.getPhenomenonUuid().equals(
                    wVolumePhenomenonUuid) && resource.getSystemName().startsWith("mblr-2")) {
                log.info("=====================================");
                log.info("Resource: {}", resource.getSystemName());
                log.debug("SystemName: {}", resource.getSystemName());
                log.debug("Name: {}", resource.getUserFriendlyName());
                
                String building = ResourceUtils.getBuilding(registryClient, resource);
                log.info("Building {}", building);
                
                //get training dataset
                final MNVDataset mnvDataset = datasetService.getDataset(resource, time2, time1);
                //get mnv data for the training dataset
                final MNVData mnv = MNVUtils.calculateFirstLevelStatistics(mnvDataset, true);
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

