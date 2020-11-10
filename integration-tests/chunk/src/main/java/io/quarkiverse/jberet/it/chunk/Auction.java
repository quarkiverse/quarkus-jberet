package io.quarkiverse.jberet.it.chunk;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Auction {
    private Long id;
    private Integer itemId;
    private Long bid;
    private Long buyout;
    private Integer quantity;
    private LocalDate processedAt;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(final Integer itemId) {
        this.itemId = itemId;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(final Long bid) {
        this.bid = bid;
    }

    public Long getBuyout() {
        return buyout;
    }

    public void setBuyout(final Long buyout) {
        this.buyout = buyout;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(final LocalDate processedAt) {
        this.processedAt = processedAt;
    }
}
