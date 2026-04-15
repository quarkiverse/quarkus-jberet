package io.quarkiverse.jberet.it.file;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.jberet.components.runtime.item.file.LineMapper;

@Singleton
@Named
public class AuctionLineMapper implements LineMapper<Auction> {
    @Override
    public Auction mapLine(String line, int lineNumber) {
        String[] parts = line.split(",");
        return new Auction(
                Long.parseLong(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Long.parseLong(parts[2].trim()),
                Long.parseLong(parts[3].trim()),
                Integer.parseInt(parts[4].trim()));
    }
}
