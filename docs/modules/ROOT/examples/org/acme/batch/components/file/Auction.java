package org.acme.batch.components.file;

public record Auction(long id, int itemId, long bid, long buyout, int quantity) {
}
