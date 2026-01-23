package org.acme.batch.chunk;

import java.io.Serializable;

import jakarta.batch.api.chunk.AbstractCheckpointAlgorithm;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class AuctionCheckpointAlgorithm extends AbstractCheckpointAlgorithm {
    @Inject
    StepContext stepContext;

    @Override
    public boolean isReadyToCheckpoint() {
        AuctionCheckpointData auctionCheckpointData = (AuctionCheckpointData) stepContext.getPersistentUserData(); // <1>
        return auctionCheckpointData.quantity() >= 1000; // <2>
    }

    public record AuctionCheckpointData(int quantity) implements Serializable {
    }
}
