package io.quarkiverse.jberet.it.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "AuctionStatistics")
public class AuctionStatistics {
    @Id
    @SequenceGenerator(name = "auctionStatisticsSeq", sequenceName = "auction_statistics_id", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auctionStatisticsSeq")
    Long id;
    Integer itemId;
    Long quantity;
    Long bid;
    Long minBid;
    Long maxBid;
    Long buyout;
    Long minBuyout;
    Long maxBuyout;
    Double avgBid;
    Double avgBuyout;
    Long timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public Long getMinBid() {
        return minBid;
    }

    public void setMinBid(Long minBid) {
        this.minBid = minBid;
    }

    public Long getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(Long maxBid) {
        this.maxBid = maxBid;
    }

    public Long getBuyout() {
        return buyout;
    }

    public void setBuyout(Long buyout) {
        this.buyout = buyout;
    }

    public Long getMinBuyout() {
        return minBuyout;
    }

    public void setMinBuyout(Long minBuyout) {
        this.minBuyout = minBuyout;
    }

    public Long getMaxBuyout() {
        return maxBuyout;
    }

    public void setMaxBuyout(Long maxBuyout) {
        this.maxBuyout = maxBuyout;
    }

    public Double getAvgBid() {
        return avgBid;
    }

    public void setAvgBid(Double avgBid) {
        this.avgBid = avgBid;
    }

    public Double getAvgBuyout() {
        return avgBuyout;
    }

    public void setAvgBuyout(Double avgBuyout) {
        this.avgBuyout = avgBuyout;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
