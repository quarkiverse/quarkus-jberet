package io.quarkiverse.jberet.deployment.devui;

import java.util.List;

import io.quarkiverse.jberet.deployment.JobBuildItem;
import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkiverse.jberet.runtime.dev.ui.JBeretDevEvents;
import io.quarkiverse.jberet.runtime.dev.ui.JBeretDevJobListener;
import io.quarkiverse.jberet.runtime.dev.ui.JBeretDevStepListener;
import io.quarkiverse.jberet.runtime.dev.ui.JBeretJsonRPCService;
import io.quarkiverse.jberet.runtime.repository.InMemoryJobRepositorySupplier;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildSteps(onlyIf = IsLocalDevelopment.class)
public class JBeretDevUIProcessor {
    @BuildStep
    CardPageBuildItem page(JBeretConfig config, List<JobBuildItem> jobs) {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addLibraryVersion("org.jberet", "jberet-core", "JBeret", "https://github.com/jberet/jsr352");
        card.addLibraryVersion("jakarta.batch", "jakarta.batch-api", "Jakarta Batch",
                "https://jakarta.ee/specifications/batch/");

        card.addBuildTimeData("xmlJobs", jobs.stream().map(job -> job.getJob().getJobXmlName()).sorted().toList());
        card.addBuildTimeData("inMemoryRepository",
                InMemoryJobRepositorySupplier.TYPE.equals(config.repository().type()));

        card.addPage(Page.webComponentPageBuilder()
                .title("Jobs")
                .icon("font-awesome-solid:list-check")
                .componentLink("qwc-jberet-jobs.js")
                .dynamicLabelJsonRPCMethodName("getJobCount"));

        card.addPage(Page.webComponentPageBuilder()
                .title("Executions")
                .icon("font-awesome-solid:clock-rotate-left")
                .componentLink("qwc-jberet-executions.js"));

        card.addPage(Page.webComponentPageBuilder()
                .title("Schedules")
                .icon("font-awesome-solid:calendar-days")
                .componentLink("qwc-jberet-schedules.js")
                .dynamicLabelJsonRPCMethodName("getScheduleCount"));

        card.addPage(Page.webComponentPageBuilder()
                .title("Batch Artifacts")
                .icon("font-awesome-solid:puzzle-piece")
                .componentLink("qwc-jberet-artifacts.js"));

        // Only shown when the Dev Assistant is configured
        card.addPage(Page.assistantPageBuilder()
                .title("Job Builder")
                .componentLink("qwc-jberet-job-builder.js"));

        return card;
    }

    @BuildStep
    FooterPageBuildItem log() {
        return new FooterPageBuildItem(Page.webComponentPageBuilder()
                .title("Batch")
                .icon("font-awesome-solid:list-check")
                .componentLink("qwc-jberet-log.js"));
    }

    @BuildStep
    AdditionalBeanBuildItem devBeans() {
        // The listeners are only referenced by name, and added to the Jobs at runtime
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(JBeretDevEvents.class, JBeretDevJobListener.class, JBeretDevStepListener.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    JsonRPCProvidersBuildItem jsonRPCService() {
        return new JsonRPCProvidersBuildItem(JBeretJsonRPCService.class);
    }
}
