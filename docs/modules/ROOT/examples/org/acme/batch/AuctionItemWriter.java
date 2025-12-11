package org.acme.batch;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.sql.DataSource;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class AuctionItemWriter extends AbstractItemWriter implements ItemWriter {
    @Inject
    DataSource dataSource;

    Connection connection;
    PreparedStatement insertStatement;

    @Override
    public void open(Serializable checkpoint) throws Exception { // <1>
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        insertStatement = connection.prepareStatement(
                "INSERT INTO auction (id, item_id, bid, buyout, quantity) VALUES (?, ?, ?, ?, ?)");
    }

    @Override
    public void writeItems(List<Object> items) throws Exception { // <2>
        for (Object item : items) {
            Auction auction = (Auction) item;
            insertStatement.setLong(1, auction.id());
            insertStatement.setString(2, auction.itemId());
            insertStatement.setLong(3, auction.bid());
            insertStatement.setLong(4, auction.buyout());
            insertStatement.setInt(5, auction.quantity());
            insertStatement.addBatch();
        }
        insertStatement.executeBatch();
    }

    @Override
    public void close() throws Exception { // <3>
        if (insertStatement != null) {
            insertStatement.close();
        }
        if (connection != null) {
            connection.close();
        }
    }
}
