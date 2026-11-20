package org.acme.batch.composition;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Decider;
import jakarta.batch.runtime.StepExecution;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class AuctionsDecider implements Decider {
    @Inject
    @BatchProperty
    Integer minimumAuctions; // <1>

    @Override
    public String decide(StepExecution[] executions) {
        StepExecution download = executions[0]; // <2>
        Integer auctions = (Integer) download.getPersistentUserData(); // <3>
        if (auctions != null && auctions >= minimumAuctions) {
            return "NEW_AUCTIONS"; // <4>
        }
        return "NO_AUCTIONS";
    }
}
