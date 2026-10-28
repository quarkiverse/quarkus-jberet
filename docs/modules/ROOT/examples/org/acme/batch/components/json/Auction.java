package org.acme.batch.components.json;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Auction(long id, int itemId, long bid, long buyout, int quantity) {
}
