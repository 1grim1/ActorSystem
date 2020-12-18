package ru.grim;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static  org.asynchttpclient.Dsl.asyncHttpClient;


public class Server{
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 2077;
    private static final String COUNT = "count";
    private static final String TEST_URL = "testUrl";
    private static final int MAP_ASYNC = 1;
    private static final int TIME_OUT = 5000;

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("Actor_system");
        ActorRef actorRef = actorSystem.actorOf(Props.create(ActorReceive.class));

        final Http http = Http.get(actorSystem);

        final ActorMaterializer actorMaterializer = ActorMaterializer.create(actorSystem);

        final Flow<HttpRequest, HttpResponse, NotUsed> flow = createFlow(http, actorSystem, actorMaterializer, actorRef);

    }

    private static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(
            Http http,
            ActorSystem actorSystem,
            ActorMaterializer actorMaterializer,
            ActorRef actorRef
    )
    {
        return Flow.of(HttpRequest.class)
                .map((request) -> {
                    Query query = request.getUri().query();
                    String url = query.get(TEST_URL).get();
                    int count = Integer.parseInt(query.get(COUNT).get());
                    return new Pair<String, Integer>(url, count);
                })
                .mapAsync(MAP_ASYNC, request -> {
                    CompletionStage<Object> completionStage = Patterns.ask(actorRef, new Message(request.first()), Duration.ofMillis(TIME_OUT));
                    return completionStage.thenCompose(res -> {
                        if((Integer) res >= 0){
                            return CompletableFuture.completedFuture(new Pair<String, Integer>(request.first(), (Integer)res));
                        }
                        Flow<Pair<String, Integer>, Integer, NotUsed> flow = Flow.<Pair<String, Integer>> create()
                                .mapConcat(pair -> new ArrayList<>(Collections.nCopies(pair.second(), pair.first())))
                                .mapAsync(request.second(), url -> {
                                    Long start = System.currentTimeMillis();
                                    asyncHttpClient().prepareGet(url).execute();
                                    Long end = System.currentTimeMillis();
                                    return CompletableFuture.completedFuture((int) (end - start));
                                });
                        return Source
                                .single(request)
                                .via(flow)
                                .toMat(Sink.fold(0, Integer::sum), Keep.right())
                                .run(actorMaterializer)
                                .thenApply(sum -> new Pair<>(request.first(), (sum / request.second())));
                    });
                })
                .map(request -> {
                    actorRef.tell(new StoreMessage(request.first(), request.second()), ActorRef.noSender());
                    return HttpResponse.create().withEntity("\t" + request.second().toString() + "\n");
                });
    }


}
