package org.acme.batch.components.mongo;

import java.time.LocalDate;

public record Auction(
        Long id,
        Integer itemId,
        Long bid,
        Long buyout,
        Integer quantity,
        LocalDate processedAt) {
}
