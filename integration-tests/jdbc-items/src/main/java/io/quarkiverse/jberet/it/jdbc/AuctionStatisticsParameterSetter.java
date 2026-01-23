package io.quarkiverse.jberet.it.jdbc;

import java.sql.SQLException;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.jdbc.ParameterSetter;

@Singleton
@Named
public class AuctionStatisticsParameterSetter implements ParameterSetter<AuctionStatistics> {
    @Override
    public void setValues(Parameters parameters, AuctionStatistics value) throws SQLException {
        parameters.setInt(1, value.itemId());
        parameters.setLong(2, value.quantity());
        parameters.setLong(3, value.bid());
        parameters.setLong(4, value.minBid());
        parameters.setLong(5, value.maxBid());
        parameters.setLong(6, value.buyout());
        parameters.setLong(7, value.minBuyout());
        parameters.setLong(8, value.maxBuyout());
        parameters.setDouble(9, value.avgBid());
        parameters.setDouble(10, value.avgBuyout());
        parameters.setLong(11, System.currentTimeMillis());
    }
}
