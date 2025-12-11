package org.acme.batch;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Optional;

import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;

@Named
public class AuctionItemReader extends AbstractItemReader implements ItemReader {
    private JsonParser parser;

    @Override
    public void open(Serializable checkpoint) throws FileNotFoundException { // <1>
        parser = Json.createParser(new FileInputStream("auctions.json"));
    }

    @Override
    public Object readItem() { // <2>
        while (parser.hasNext()) {
            JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.START_OBJECT) {
                JsonObject auction = parser.getObject();
                Long id = auction.getJsonNumber("id").longValue();
                String item = auction.getString("item");
                Long bid = Optional.ofNullable(auction.getJsonNumber("bid")).map(JsonNumber::longValue).orElse(0L);
                Long buyout = Optional.ofNullable(auction.getJsonNumber("buyout")).map(JsonNumber::longValue).orElse(0L);
                Integer quantity = auction.getInt("quantity");
                return new Auction(id, item, bid, buyout, quantity);
            }
        }
        return null;
    }

    @Override
    public void close() { // <3>
        parser.close();
    }
}
