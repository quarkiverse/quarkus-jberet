package org.acme.batch.components.jdbc;

import javax.sql.DataSource;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.item.jdbc.JdbcBatchItemWriter;

@Singleton
public class AuctionJdbcBatchItemWriterProducer {
    @Inject
    DataSource dataSource;

    @Inject
    AuctionStatisticsParameterSetter parameterSetter;

    @Produces
    @Dependent
    @Named("auctionsItemWriter")
    public JdbcBatchItemWriter<AuctionStatistics> auctionsItemWriter() {
        String sql = """
                INSERT INTO AuctionStatistics (
                    id, itemId, quantity, bid, minBid, maxBid,
                    buyout, minBuyout, maxBuyout, avgBid, avgBuyout, timestamp
                ) VALUES (nextval('auction_statistics_id'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return new JdbcBatchItemWriter<>(dataSource, sql, parameterSetter);
    }
}
