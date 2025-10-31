package io.quarkiverse.jberet.it.jdbc;

import javax.sql.DataSource;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.item.jdbc.JdbcBatchItemWriter;
import io.quarkiverse.jberet.components.item.jdbc.JdbcCursorItemReader;

@Singleton
public class AuctionStatisticsProducer {
    @Inject
    DataSource dataSource;
    @Inject
    AuctionStatisticsRowMapper rowMapper;
    @Inject
    AuctionStatisticsParameterSetter parameterSetter;

    @Produces
    @Dependent
    @Named("auctionsItemReader")
    JdbcCursorItemReader<AuctionStatistics> auctionsItemReader() {
        String sql = """
                SELECT
                itemId,
                sum(quantity),
                sum(bid),
                sum(buyout),
                min(bid / quantity),
                min(buyout / quantity),
                max(bid / quantity),
                max(buyout / quantity)
                FROM Auctions
                GROUP BY itemId
                ORDER BY 1
                """;
        return new JdbcCursorItemReader<>(dataSource, sql, rowMapper)
                .setConnectionAutoCommit(false)
                .setFetchSize(100);
    }

    @Produces
    @Dependent
    @Named("auctionsItemWriter")
    JdbcBatchItemWriter<AuctionStatistics> auctionsItemWriter() {
        String sql = """
                INSERT INTO AuctionStatistics (
                id,
                itemId,
                quantity,
                bid,
                minBid,
                maxBid,
                buyout,
                minBuyout,
                maxBuyout,
                avgBid,
                avgBuyout,
                timestamp
                ) VALUES (nextval('auction_statistics_id'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return new JdbcBatchItemWriter<>(dataSource, sql, parameterSetter);
    }
}
