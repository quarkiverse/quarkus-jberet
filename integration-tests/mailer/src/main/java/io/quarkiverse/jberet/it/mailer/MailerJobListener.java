package io.quarkiverse.jberet.it.mailer;

import jakarta.batch.api.listener.AbstractJobListener;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

@Named
public class MailerJobListener extends AbstractJobListener {
    @Inject
    JobContext jobContext;
    @Inject
    Mailer mailer;

    @Override
    public void afterJob() {
        if (jobContext.getBatchStatus().equals(BatchStatus.FAILED)) {
            mailer.send(Mail.withText("batch-error@quarkiverse.org", "Batch Error", "Batch Mailer failed")
                    .setFrom("mailer-test@quarkiverse.org"));
        }
    }
}
