package demostoregatling.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public final class CmsPages {
    public static final ChainBuilder home = exec(
            http("Load Home Page")
                    .get("/")
                    .check(substring("Welcome to the Gatling DemoStore!").exists())
                    .check(css("#_csrf", "content").saveAs("csrfToken"))
    );

    public static final ChainBuilder about_us_page = exec
            (http("Load About Us Page")
                    .get("/about-us")
                    .check(regex("<h2>About Us</h2>").exists())
            );
}
