package org.acme.batch.components.jpa;

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
}
