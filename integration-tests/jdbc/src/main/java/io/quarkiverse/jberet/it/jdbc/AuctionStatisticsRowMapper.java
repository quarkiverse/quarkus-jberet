package io.quarkiverse.jberet.it.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.item.jdbc.RowMapper;

@Singleton
@Named
public class AuctionStatisticsRowMapper implements RowMapper<AuctionStatistics> {
    @Override
    public AuctionStatistics mapRow(final ResultSet resultSet) throws SQLException {
        int itemId = resultSet.getInt(1);
        long quantity = resultSet.getLong(2);
        long bid = resultSet.getLong(3);
        long buyout = resultSet.getLong(4);
        long minBid = resultSet.getLong(5);
        long minBuyout = resultSet.getLong(6);
        long maxBid = resultSet.getLong(7);
        long maxBuyout = resultSet.getLong(8);
        Double avgBid = (double) (bid / quantity);
        Double avgBuyout = (double) (buyout / quantity);
        return new AuctionStatistics(itemId, quantity, bid, minBid, maxBid, buyout, minBuyout, maxBuyout, avgBid, avgBuyout);
    }
}
