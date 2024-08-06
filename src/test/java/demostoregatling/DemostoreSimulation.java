package demostoregatling;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;


import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import demostoregatling.pageobjects.Catalog;
import demostoregatling.pageobjects.Checkout;
import demostoregatling.pageobjects.CmsPages;
import demostoregatling.pageobjects.Customer;

public class DemostoreSimulation extends Simulation {

    private static final String DOMAIN = "demostore.gatling.io";
    private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("https://" + DOMAIN);

    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "3"));

    private static final Duration RAMP_DURATION = Duration.ofSeconds(Integer.parseInt(System.getProperty("RAMP_DURATION", "10")));
    private static final Duration TEST_DURATION = Duration.ofSeconds(Integer.parseInt(System.getProperty("DURATION", "60")));

    @Override
    public void before(){
        System.out.printf("Running test with %d users%n", USER_COUNT);
        System.out.printf("Ramping users over %d seconds%n",RAMP_DURATION.getSeconds());
        System.out.printf("Total test duration: %d seconds%n",TEST_DURATION.getSeconds());
    }

    @Override
    public void after(){
        System.out.println("Stress test completed.");
    }

    private static final ChainBuilder initSession =
            exec(flushCookieJar())
                    .exec(session -> session.set("randomNumber", ThreadLocalRandom.current().nextInt()))
                    .exec(session -> session.set("customerLoggedIn", false))
                    .exec(session -> session.set("cartTotal", 0.00))
                    .exec(addCookie(Cookie("sessionId", SessionId.random()).withDomain(DOMAIN)))
                    .exec(
                            session -> {
                                //System.out.println(session.toString());
                                return session;
                            }
                    );

    private static class UserJourneys {
        private static final Duration MIN_PAUSE_DURATION = Duration.ofMillis(100);
        private static final Duration MAX_PAUSE_DURATION = Duration.ofMillis(500);

        private static final ChainBuilder browseStore =
                exec(initSession)
                        .exec(CmsPages.home)
                        .pause(MAX_PAUSE_DURATION)
                        .exec(CmsPages.about_us_page)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .repeat(5)
                        .on(
                                exec(Catalog.Categories.view)
                                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                                        .exec(Catalog.Products.view)
                        );

        private static final ChainBuilder abandonCart =
                exec(initSession)
                        .exec(CmsPages.home)
                        .pause(MAX_PAUSE_DURATION)
                        .exec(Catalog.Categories.view)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .exec(Catalog.Products.view)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .exec(Catalog.Products.add_to_cart);

        private static final ChainBuilder completePurchase =
                exec(initSession)
                        .exec(CmsPages.home)
                        .pause(MAX_PAUSE_DURATION)
                        .exec(Catalog.Categories.view)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .exec(Catalog.Products.view)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .exec(Catalog.Products.add_to_cart)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .exec(Checkout.viewCart)
                        .pause(MIN_PAUSE_DURATION, MAX_PAUSE_DURATION)
                        .exec(Checkout.complete_checkout);
    }

    private static class Scenarios {
        private static final ScenarioBuilder defaultPurchase =
                scenario("Default Load Test")
                        .during(TEST_DURATION)
                        .on(
                                randomSwitch()
                                        .on(
                                                new Choice.WithWeight(75.0, exec(UserJourneys.browseStore)),
                                                new Choice.WithWeight(15.0, exec(UserJourneys.abandonCart)),
                                                new Choice.WithWeight(10.0, exec(UserJourneys.completePurchase))));

        private static final ScenarioBuilder highPurchase =
                scenario("High Purchase Load Test")
                        .during(Duration.ofSeconds(60))
                        .on(
                                randomSwitch()
                                        .on(
                                                new Choice.WithWeight(25.0, exec(UserJourneys.browseStore)),
                                                new Choice.WithWeight(25, exec(UserJourneys.abandonCart)),
                                                new Choice.WithWeight(50.0, exec(UserJourneys.completePurchase))));
    }

    {
        setUp(
                Scenarios.defaultPurchase
                        .injectOpen(rampUsers(USER_COUNT).during(RAMP_DURATION)).protocols(HTTP_PROTOCOL),
                Scenarios.highPurchase
                        .injectOpen(rampUsers(2).during(Duration.ofSeconds(10))).protocols(HTTP_PROTOCOL));
    }
}