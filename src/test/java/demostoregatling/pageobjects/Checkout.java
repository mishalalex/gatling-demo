package demostoregatling.pageobjects;

import demostoregatling.DemostoreSimulation;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Checkout {
    public static final ChainBuilder complete_checkout = exec(
            http("Load Checkout Page")
                    .get("/cart/checkout")
                    .check(substring("Thanks for your order! See you soon!").exists())
    );

    public static final ChainBuilder viewCart =
            doIf(session -> !session.getBoolean("customerLoggedIn"))
                    .then(exec(Customer.Login.login))
                    .exec(http("Load Cart Page")
                            .get("/cart/view")
                            .check(css("#grandTotal").isEL("$#{cartTotal}")));
}
