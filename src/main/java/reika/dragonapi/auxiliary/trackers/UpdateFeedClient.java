package reika.dragonapi.auxiliary.trackers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import reika.dragonapi.extras.ModVersion;

/** Original name:version feed format, with bounded transport and explicit non-version failures. */
public final class UpdateFeedClient {
    public enum Status { VERIFIED, UNAVAILABLE, MALFORMED, MISSING, UNCONFIGURED }
    public record Result(Status status, Optional<ModVersion> version, Optional<Instant> modified, String detail) {
        public static Result failure(Status status, String detail) { return new Result(status,Optional.empty(),Optional.empty(),detail); }
    }
    public static final int MAX_BYTES = 65536;
    private UpdateFeedClient() {}
    public static String label(String name) { return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT); }
    public static Result fetch(URI uri, String name) { return fetch(uri,name,10000); }
    public static Result fetch(URI uri, String name, int timeoutMillis) {
        if (timeoutMillis <= 0) throw new IllegalArgumentException("Timeout must be positive");
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            return Result.failure(Status.MALFORMED,"Feed must use HTTP or HTTPS");
        HttpURLConnection connection = null;
        long deadline = System.nanoTime() + timeoutMillis * 1000000L;
        try {
            connection = (HttpURLConnection)uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false); connection.setUseCaches(false);
            connection.setConnectTimeout(timeoutMillis); connection.setReadTimeout(timeoutMillis);
            connection.setRequestProperty("Accept","text/plain");
            connection.connect(); connection.setReadTimeout(remaining(deadline));
            int status = connection.getResponseCode();
            if (status != 200) return Result.failure(Status.UNAVAILABLE,"HTTP " + status);
            long length = connection.getContentLengthLong();
            if (length > MAX_BYTES) return Result.failure(Status.MALFORMED,"Feed exceeds size limit");
            var bytes = new ByteArrayOutputStream();
            try (var input = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                while (true) {
                    connection.setReadTimeout(remaining(deadline));
                    int read = input.read(buffer);
                    if (read < 0) break;
                    if (bytes.size() + read > MAX_BYTES) return Result.failure(Status.MALFORMED,"Feed exceeds size limit");
                    bytes.write(buffer,0,read);
                }
            }
            Result parsed = parse(bytes.toByteArray(),name);
            long modified = connection.getLastModified();
            return new Result(parsed.status,parsed.version,modified <= 0 ? Optional.empty() : Optional.of(Instant.ofEpochMilli(modified)),parsed.detail);
        } catch (IOException | IllegalArgumentException error) {
            return Result.failure(Status.UNAVAILABLE,error.getClass().getSimpleName() + ": " + error.getMessage());
        } finally { if (connection != null) connection.disconnect(); }
    }
    private static int remaining(long deadline) throws SocketTimeoutException {
        long nanos = deadline - System.nanoTime();
        if (nanos <= 0) throw new SocketTimeoutException("Update feed deadline exceeded");
        return (int)Math.max(1,(nanos + 999999) / 1000000);
    }
    public static Result parse(byte[] bytes, String name) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) return Result.failure(Status.MALFORMED,"Empty or oversized feed");
        String text;
        try { text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (java.nio.charset.CharacterCodingException error) { return Result.failure(Status.MALFORMED,"Invalid UTF-8"); }
        ModVersion found = null;
        for (String row : text.split("\\R")) {
            row = row.strip(); if (row.isEmpty() || row.startsWith("#")) continue;
            int colon = row.indexOf(':');
            if (colon <= 0) return Result.failure(Status.MALFORMED,"Malformed version row");
            if (!label(row.substring(0,colon)).equals(label(name))) continue;
            if (found != null) return Result.failure(Status.MALFORMED,"Duplicate mod entry");
            String raw = row.substring(colon+1).strip();
            ModVersion version = ModVersion.getFromString(raw);
            if (!version.verify() || !version.isCompiled() || version.majorVersion < 0)
                return Result.failure(Status.MALFORMED,"Invalid version for " + name);
            found = version;
        }
        return found == null ? Result.failure(Status.MISSING,"No entry for " + name)
                : new Result(Status.VERIFIED,Optional.of(found),Optional.empty(),"");
    }
}
