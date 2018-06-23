package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;

public class ControllerBalanceHistoryTest {

    @BeforeClass
    public static void before() {
        RestAssured.basePath = "api/v1";
    }

    @Test
    public void testBalanceHistory() {
        //Only param
        Response response = given()
                .param("session_id", 1)
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(200);
        assertNotNull(response.jsonPath().getFloat("open"));
        //Only header
        response = given()
                .header("X-session-ID", 1)
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(200);
        assertNotNull(response.jsonPath().getFloat("open"));
        //No param or header
        response = given()
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(401);
        //Invalid input
        response = given()
                .header("X-session-ID", 1)
                .param("interval", "invalid")
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(405);
        response = given()
                .header("X-session-ID", 1)
                .param("intervals", 201)
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(405);
        response = given()
                .header("X-session-ID", 1)
                .param("intervals", -1)
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(405);
        //Header and param are different
        response = given()
                .header("X-session-ID", 1)
                .param("session_id", 2)
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(401);
        //Intervals are given correctly
        response = given()
                .header("X-session-ID", 1)
                .param("interval", "day")
                .param("intervals", 10)
                .when()
                .get("/balance/history");
        response.then().assertThat().statusCode(200);
    }

}
