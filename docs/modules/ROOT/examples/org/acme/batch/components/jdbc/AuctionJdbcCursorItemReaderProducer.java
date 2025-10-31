package org.acme.batch.components.jdbc;

import javax.sql.DataSource;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.item.jdbc.JdbcCursorItemReader;

@Singleton
public class AuctionJdbcCursorItemReaderProducer {
    @Inject
    DataSource dataSource;
    @Inject
    AuctionStatisticsRowMapper rowMapper;

    @Produces
    @Dependent
    @Named("auctionsItemReader")
    public JdbcCursorItemReader<AuctionStatistics> auctionsItemReader() {
        String sql = """
                SELECT
                    itemId,
                    sum(quantity) as totalQuantity,
                    sum(bid) as totalBid,
                    sum(buyout) as totalBuyout,
                    min(bid / quantity) as minBid,
                    min(buyout / quantity) as minBuyout,
                    max(bid / quantity) as maxBid,
                    max(buyout / quantity) as maxBuyout
                FROM Auctions
                GROUP BY itemId
                ORDER BY itemId
                """;
        return new JdbcCursorItemReader<>(dataSource, sql, rowMapper);
    }
}
