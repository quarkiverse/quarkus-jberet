package org.acme.batch.chunk;

import jakarta.batch.api.chunk.listener.AbstractItemReadListener;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

@Named
public class AuctionItemReadListener extends AbstractItemReadListener {
    private static final Logger LOG = Logger.getLogger(AuctionItemReadListener.class);

    @Override
    public void afterRead(Object item) {
        LOG.info("Read item: " + item);
    }
}
