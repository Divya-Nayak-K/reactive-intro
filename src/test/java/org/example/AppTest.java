package org.example;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class AppTest {
    public List<String> rawString;
    public Flux<String> publisher;

    @BeforeEach
    void init() {
        this.rawString = List.of("And", "Because", "Could");
        this.publisher = Flux.fromIterable(this.rawString);
    }

    @Test
    public void shouldAnswerWithTrue() {
        rawString.forEach(System.out::println);
    }

    @Test
    void simpleSubscriber() {
        publisher.subscribe(System.out::println);
    }

    @Test
    void simpleSubscriberWithEvent() {
        publisher.log().subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("Completed Event")

        );
    }

    @Test
    void singleValuePublisher() {
        Mono<String> monoPublisher = Mono.justOrEmpty("This is a mono");
        monoPublisher.log().subscribe(System.out::println);
    }

    @Test
    void simpleErrorHandling() {
        Flux<Integer> integerFlux = Flux.range(1, 20).map(integer -> {
            if (integer == 5) {
                throw new RuntimeException("Exception Found Number 5");
            }
            return integer;
        });
        integerFlux.subscribe(System.out::println,
                System.err::println);
    }

    @Test
    void simpleCancellationSubscription() {
        Flux<Integer> delayElements = Flux.range(1, 20).delayElements(Duration.ofSeconds(2));

        Disposable disposable = delayElements.subscribe(System.out::println,
                System.err::println,
                () -> System.out.println("Completed Event"));

        Runnable runnable = () -> {
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Cancelling subs");
            disposable.dispose();
        };
        runnable.run();
    }

    @Test
    void connectSubscriptions() {
        ConnectableFlux<Integer> connectableFlux = Flux.range(5, 10).publish();
        connectableFlux.subscribe(s -> System.out.println("First " + s));
        connectableFlux.subscribe(s -> System.out.println("Second " + s));
        connectableFlux.subscribe(s -> System.out.println("Third " + s));
        connectableFlux.connect();

    }

    @Test
    void publishDirectly() {
        Flux<String> strPublisher = Flux.generate(
                AtomicInteger::new,
                (mutableInt, publishIt) -> {
                    publishIt.next(String.format(" on next value:: %s",
                            mutableInt.getAndIncrement()));
                    if (mutableInt.get() == 17) {
                        publishIt.complete();
                    }
                    return mutableInt;
                });
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification"));
    }

    @Test
    void concurrentPublishing() {
        Scheduler reactScheduler = Schedulers.newParallel("pub  parallel", 4);
        final Flux<String> phrasePublish = Flux.range(1, 20)
                .map(i -> 42 + i)
                .publishOn(reactScheduler)
                .map(m -> {
                    var v = Thread.currentThread().getName();
                    return String.format("%s value produced::%s", v, m);
                });

        Runnable r = () -> phrasePublish.subscribe(System.out::println);
        r.run();

        Runnable r1 = () -> phrasePublish.subscribe(System.out::println);
        r1.run();

        Runnable r2 = () -> phrasePublish.subscribe(System.out::println);
        r2.run();
    }

}