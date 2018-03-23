import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.jayway.restassured.response.Response;
import org.junit.runner.RunWith;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@RunWith(Ginkgo4jRunner.class)
public class MicroserviceAcceptanceTests {

    private static final String GATEWAY;

    static {
        String gateway = System.getenv("GATEWAY");
        if (gateway == null) {
            gateway = "http://localhost:8080";
        }
        GATEWAY = gateway;
    }

    private String username;
    private String password;

    private Response response;
    private String xrsfToken;
    private String accessToken;

    {
        Describe("MicroserviceAcceptanceTests", () -> {
            Context("given an xsrf token", () -> {
                BeforeEach(() -> {
                    response =
                            given().
                                    contentType("application/json").
                            when().
                                    get(format("%s/", GATEWAY)).
                            then().
                                    extract().response();

                    xrsfToken = response.cookie("XSRF-TOKEN");

                });
                Context("given we are unauthenticated", () -> {
                    BeforeEach(() -> {
                        response =
                            given().
                                    contentType("application/json").
                                    cookie("XSRF-TOKEN", xrsfToken).
                                    header("X-XSRF-TOKEN", xrsfToken).
                                when().
                                    get(format("%s/uaa/api/account", GATEWAY)).
                                then().
                                    extract().response();

                        assertThat(response.getStatusCode(), is(401));
                    });
                    Context("given valid credentials", () -> {
                        BeforeEach(() -> {
                            username = "user";
                            password = "user";
                        });
                        Context("when authentication happens", () -> {
                            BeforeEach(() -> {
                                response = given().
                                            contentType("application/json").
                                            cookie("XSRF-TOKEN", xrsfToken).
                                            header("X-XSRF-TOKEN", xrsfToken).
                                            body(format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password)).
                                        when().
                                            post(format("%s/auth/login", GATEWAY)).
                                        then().
                                            extract().response();
                            });
                            It("should succeed", () -> {
                                assertThat(response.getStatusCode(), is(200));
                            });
                            It("should return an oauth2 token", () -> {
                                assertThat(response.cookie("access_token"), is(not(nullValue())));
                            });
                            Context("given an access token", () -> {
                                BeforeEach(() -> {
                                    accessToken = response.cookie("access_token");
                                });
                                It("should be able to access an authenticated resource", () -> {
                                    response =
                                            given().
                                                    contentType("application/json").
                                                    cookie("XSRF-TOKEN", xrsfToken).
                                                    cookie("access_token", accessToken).
                                                    when().
                                                    get("http://localhost:8080/uaa/api/account").
                                                    then().extract().response();

                                    assertThat(response.getStatusCode(), is(200));
                                });
                            });
                        });
                    });
                    Context("given an invalid credentials", () -> {
                        BeforeEach(() -> {
                            username = "user";
                            password = "badness";
                        });
                        Context("when authentication happens", () -> {
                            BeforeEach(() -> {
                                response = given().
                                            contentType("application/json").
                                            cookie("XSRF-TOKEN", xrsfToken).
                                            header("X-XSRF-TOKEN", xrsfToken).
                                        body(format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password)).
                                        when().
                                            post(format("%s/auth/login", GATEWAY)).
                                        then().extract().response();
                            });
                            It("should fail", () -> {
                                assertThat(response.getStatusCode(), is(500));
                            });
                        });
                    });
                });
            });
        });
    }
}
