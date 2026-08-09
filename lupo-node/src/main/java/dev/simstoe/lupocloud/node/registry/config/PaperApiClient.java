package dev.simstoe.lupocloud.node.registry.config;

import dev.simstoe.lupocloud.api.models.ServiceType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class PaperApiClient {
    private static final String BASE_URL = "https://fill.papermc.io/v3/projects/";

    private static final Pattern LATEST_VERSION =
            Pattern.compile("\"versions\"\\s*:\\s*\\{\\s*\"[^\"]+\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
    private static final Pattern BUILD_DOWNLOAD = Pattern.compile(
            "\"downloads\"\\s*:\\s*\\{\\s*\"server:default\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\".*?\"url\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record ResolvedBuild(String fileName, String url) {}

    public ResolvedBuild resolveLatestBuild(ServiceType type) throws IOException, InterruptedException {
        String latestVersion = fetchLatestVersion(type.getProjectId());
        return fetchLatestBuild(type.getProjectId(), latestVersion);
    }

    private String fetchLatestVersion(String projectId) throws IOException, InterruptedException {
        String body = get(BASE_URL + projectId);
        Matcher matcher = LATEST_VERSION.matcher(body);
        if (!matcher.find()) {
            throw new IOException("Cloud not find the version of '" + projectId + "'");
        }
        return matcher.group(1);
    }

    private ResolvedBuild fetchLatestBuild(String projectId, String version) throws IOException, InterruptedException {
        String body = get(BASE_URL + projectId + "/versions/" + version + "/builds/latest");
        Matcher matcher = BUILD_DOWNLOAD.matcher(body);
        if (!matcher.find()) {
            throw new IOException("Could not read download-information for " + projectId + " " + version + ".");
        }
        return new ResolvedBuild(matcher.group(1), matcher.group(2));
    }

    private String get(String url) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpectedly status-code " + response.statusCode() + " of " + url);
        }
        return response.body();
    }
}
