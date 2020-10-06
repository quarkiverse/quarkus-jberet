package io.quarkiverse.jberet.it;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;

import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
@Named
@Transactional
public class AuctionItemReader implements ItemReader {
    private JsonParser parser;
    private InputStream in;

    @Inject
    @ConfigProperty(name = "auctions.file", defaultValue = "/auctions.json")
    String auctionsFile;

    @Override
    public void open(Serializable checkpoint) {
        in = AuctionItemReader.class.getResourceAsStream(auctionsFile);
        setParser(Json.createParser(in));
    }

    @Override
    public void close() throws Exception {
        if (in != null) {
            in.close();
        }
    }

    @Override
    public Object readItem() {
        while (parser.hasNext()) {
            Auction auction = new Auction();
            if (readAuctionItem(auction)) {
                return auction;
            }
        }
        return null;
    }

    @Override
    public Serializable checkpointInfo() {
        return null;
    }

    protected boolean readAuctionItem(Auction auction) {
        JsonParser.Event event = parser.next();
        if (event == JsonParser.Event.START_OBJECT) {
            final JsonObject object = parser.getObject();
            auction.setId(object.getJsonNumber("id").longValue());
            auction.setItemId(object.getJsonObject("item").getInt("id"));
            auction.setBid(Optional.ofNullable(object.getJsonNumber("bid")).map(JsonNumber::longValue).orElse(0L));
            auction.setBuyout(Optional.ofNullable(object.getJsonNumber("buyout")).map(JsonNumber::longValue).orElse(0L));
            auction.setQuantity(object.getInt("quantity"));
            return true;
        }
        return false;
    }

    public void setParser(JsonParser parser) {
        this.parser = parser;
        searchAuctions();
    }

    private void searchAuctions() {
        while (parser.hasNext()) {
            JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.KEY_NAME && parser.getString().equalsIgnoreCase("auctions")) {
                parser.next();
                break;
            }
        }
    }
}
