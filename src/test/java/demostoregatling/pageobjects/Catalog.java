package demostoregatling.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class Catalog {
    private static final FeederBuilder<String> categoryFeed = csv("data/categoryDetails.csv").random();
    private static final FeederBuilder<Object> productFeed = jsonFile("data/productDetails.json").random();

    public static class Categories {
        public static final ChainBuilder view =
                feed(categoryFeed)
                        .exec(http("Load Category Page - #{categoryName}")
                                .get("/category/#{categorySlug}")
                                .check(css("#CategoryName").isEL("#{categoryName}"))
                        );
    }

    public static class Products {
        public static final ChainBuilder view =
                feed(productFeed)
                        .exec(http("Load Product Page - #{productName}")
                                .get("/product/#{slug}")
                                .check(css("#ProductDescription").isEL("#{description}"))
                        );

        public static final ChainBuilder add_to_cart =
                exec(view)
                        .exec(http("Add '#{productName}' product to Cart")
                                .get("/cart/add/#{productId}")
                                .check(substring(" items in your cart.")))
                        .exec(
                                session -> {
                                    double currentCartTotal = session.getDouble("cartTotal");
                                    double itemPrice = session.getDouble("price");
                                    return session.set("cartTotal", currentCartTotal + itemPrice);
                                }
                        )
                        .exec(
                                session -> {
                                    //System.out.println("Cart Total: $" + Objects.requireNonNull(session.get("cartTotal")));
                                    return session;
                                }
                        );
    }
}
