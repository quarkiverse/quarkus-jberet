package io.quarkiverse.jberet.it.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/auctions")
@Produces(MediaType.APPLICATION_JSON)
public class FileResource {
    @GET
    @jakarta.ws.rs.Path("/output/count")
    public Response count() throws IOException {
        Path output = Path.of("target/auctions-output.csv");
        if (!Files.exists(output)) {
            return Response.ok(0).build();
        }
        List<String> lines = Files.readAllLines(output);
        return Response.ok(lines.size()).build();
    }
}
