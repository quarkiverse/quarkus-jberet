package io.quarkiverse.jberet.it.mailer;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.inject.Named;

@Named
public class MailerBatchlet extends AbstractBatchlet {
    @Override
    public String process() {
        throw new RuntimeException();
    }
}
