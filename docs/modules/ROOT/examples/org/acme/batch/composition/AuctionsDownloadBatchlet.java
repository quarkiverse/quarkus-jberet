package org.acme.batch.composition;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.stream.JsonParser;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Named
public class AuctionsDownloadBatchlet extends AbstractBatchlet {
    @Inject
    StepContext stepContext;
    @Inject
    @ConfigProperty(name = "auctions.url")
    String url;

    @Override
    public String process() throws Exception {
        int auctions = download(); // <1>
        stepContext.setPersistentUserData(auctions); // <2>
        if (auctions == 0) {
            return "NO_AUCTIONS"; // <3>
        }
        return BatchStatus.COMPLETED.toString(); // <4>
    }

    private int download() throws Exception {
        Path file = Paths.get("auctions.json");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofFile(file));

        int auctions = 0;
        try (InputStream inputStream = Files.newInputStream(file);
                JsonParser parser = Json.createParser(inputStream)) {
            while (parser.hasNext()) {
                if (parser.next() == JsonParser.Event.START_OBJECT) {
                    auctions++;
                }
            }
        }
        return auctions;
    }
}
