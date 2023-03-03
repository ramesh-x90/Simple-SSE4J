package org.simplesseclient.client;

import com.google.gson.Gson;
import lombok.Builder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;


@Builder
public class SimpleSSEClient {
    @Builder.Default
    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(
                    Duration
                            .of(10, TimeUnit.SECONDS.toChronoUnit()
                            )
            )
            .build();


    public <T> CompletableFuture<EventSource<T>> subscribe(String uri, Class<T> dataType) {

        try {
            HttpRequest request = HttpRequest
                    .newBuilder(new URI(uri))
                    .build();

            CompletableFuture<EventSource<T>> future = httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(inputStreamHttpResponse -> {
                        int statusCode = inputStreamHttpResponse.statusCode();
                        if (statusCode != 200) {
                            System.out.println("Error Code: " + statusCode);
                        }
                        return inputStreamHttpResponse;
                    })
                    .thenApply(HttpResponse::body)
                    .thenApply(inputStream ->
                            new BufferedReader(
                                    new InputStreamReader(
                                            new BufferedInputStream(inputStream)
                                    )
                            )


                    )
                    .thenApply(bufferedReader -> {
                        Stream<String> stream = bufferedReader.lines();
                        AtomicReference<StringBuilder> stringBuilder = new AtomicReference<>(new StringBuilder());
                        String dataFlag = "data:";
                        Gson gson = new Gson();
                        return stream.map(
                                        s -> {

                                            if (s.startsWith(dataFlag)) {
                                                stringBuilder.get().append(s.substring(dataFlag.length())).toString().trim();
                                            }

                                            if (s.trim().isBlank()) {
                                                String str = stringBuilder.get().toString();
                                                stringBuilder.set(new StringBuilder());
                                                return str;
                                            }
                                            return null;
                                        }
                                )
                                .filter(Objects::nonNull)
                                .filter(s -> !s.isBlank())
                                .map(s -> gson.fromJson(s, dataType))
                                .iterator();
                    })
                    .thenApply(EventSource::new);

            return future;


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        return null;
    }


}
