package demostoregatling.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Customer {
    private static final FeederBuilder<String> loginFeed = csv("data/loginDetails.csv").random();

    public static class Login {
        public static final ChainBuilder login =
                feed(loginFeed)
                        .exec(http("Logging as #{username}")
                                .post("/login")
                                .formParam("_csrf", "#{csrfToken}")
                                .formParam("username", "#{username}")
                                .formParam("password", "#{password}")
                        )
                        .exec(session -> session.set("customerLoggedIn", true));
    }
}