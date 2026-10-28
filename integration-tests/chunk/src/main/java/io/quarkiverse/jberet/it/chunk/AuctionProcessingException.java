package io.quarkiverse.jberet.it.chunk;

/**
 * Not registered for serialization, to verify that step failures with any exception type are handled in native mode.
 */
public class AuctionProcessingException extends RuntimeException {
    public AuctionProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
