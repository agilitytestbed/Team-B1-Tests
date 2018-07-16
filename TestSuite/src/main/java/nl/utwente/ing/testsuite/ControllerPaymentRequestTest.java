package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ControllerPaymentRequestTest {

    @BeforeClass
    public static void setUp() {
        RestAssured.basePath = "api/v1";
    }

    @Test
    public void testGET() {
        //no param or header
        given().
                when().
                get("/paymentRequests").
                then().
                assertThat().
                statusCode(401);
        //only param
        given().
                param("session_id", 1).
                when().
                get("/paymentRequests").
                then().
                assertThat().
                statusCode(200);
        //correct header
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(200);
        //different param and header
        given()
                .header("X-session-ID", 2)
                .param("session_id", 1)
                .when()
                .get("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testPOST() {
        JSONObject jsonObject = new JSONObject().put("description", "something").put("due_date", LocalDateTime.now().toString())
                .put("amount", 123).put("number_of_requests", 2);
        //no param or header
        given()
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(401);
        //no body
        given()
                .header("X-session-ID", 1)
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(405);
        //successful post
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(201);
        JsonPath jsonPath = given()
                .header("X-session-ID", 1)
                .when()
                .get("/paymentRequests")
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .response()
                .jsonPath();
        List<Object> list = jsonPath.getList("description");
        assertEquals("something", list.get(list.size() - 1));
        //add new payment request for non existent session
        given()
                .header("X-session-ID", -1)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(401);
        //wrong json field
        jsonObject.remove("description");
        jsonObject.put("decription", "somethign");
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(405);
        //field is missing
        jsonObject.remove("decription");
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(405);
    }

    @Test
    public void testUpdateTransactions() {
        JSONObject jsonObject = new JSONObject()
                .put("description", "something")
                .put("due_date", LocalDateTime.now().plusYears(1))
                .put("amount", 125)
                .put("number_of_requests", 2);
        given()
                .header("X-session-ID", 2)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(201);
        jsonObject = new JSONObject()
                .put("date", LocalDateTime.now().toString())
                .put("amount", 125)
                .put("externalIBAN", "NL89INGB0258036901")
                .put("description", "something")
                .put("type", "deposit");
        given()
                .header("X-session-ID", 2)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        JsonPath jsonPath = given()
                .header("X-session-ID", 2)
                .when()
                .get("/paymentRequests")
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .response()
                .jsonPath();
        String string = jsonPath.getString("transactions.id");
        jsonPath = given()
                .header("X-session-ID", 2)
                .when()
                .get("/transactions")
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .response()
                .jsonPath();
        List<Object> list = jsonPath.getList("id");
        int lastID = (int) list.get(list.size() - 1);
        assertTrue(string.contains(String.valueOf(lastID)));
    }

}
