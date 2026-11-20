package org.acme.batch.composition;

import java.util.List;
import java.util.Properties;

import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionPlanImpl;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Named
public class RegionPartitionMapper implements PartitionMapper {
    @Inject
    @ConfigProperty(name = "auctions.regions")
    List<String> regions; // <1>

    @Override
    public PartitionPlan mapPartitions() {
        Properties[] partitionProperties = new Properties[regions.size()];
        for (int i = 0; i < regions.size(); i++) {
            partitionProperties[i] = new Properties();
            partitionProperties[i].setProperty("region", regions.get(i)); // <2>
        }

        PartitionPlan partitionPlan = new PartitionPlanImpl();
        partitionPlan.setPartitions(regions.size()); // <3>
        partitionPlan.setThreads(2); // <4>
        partitionPlan.setPartitionProperties(partitionProperties);
        return partitionPlan;
    }
}
