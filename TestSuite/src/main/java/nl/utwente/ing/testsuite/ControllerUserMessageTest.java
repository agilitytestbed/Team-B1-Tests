package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class ControllerUserMessageTest {


    @BeforeClass
    public static void setUp() {
        RestAssured.basePath = "api/v1";
    }

    @Test
    public void testGET() {
        //no header or param
        given()
                .when()
                .get("/messages")
                .then()
                .assertThat()
                .statusCode(401);
        //good param
        given()
                .param("session_id", 1)
                .when()
                .get("/messages")
                .then()
                .assertThat()
                .statusCode(200);
        //good header
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/messages")
                .then()
                .assertThat()
                .statusCode(200);
        //mismatching header and param
        given()
                .header("X-session-ID", 1)
                .param("session_id", 2)
                .when()
                .get("/messages")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testPUT() {
        //no param or header
        given()
                .header("Content-Type", "application/json")
                .when()
                .put("/messages/1")
                .then()
                .assertThat()
                .statusCode(401);
        //invalid message
        given()
                .header("X-session-ID", 1)
                .header("Content-Type", "application/json")
                .when()
                .put("/messages/-1")
                .then()
                .assertThat()
                .statusCode(404);
        JsonPath jsonPath = given()
                .header("X-session-ID", 1)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list = jsonPath.getList("id");
        int lastId = (int) list.get(list.size() - 1);
        System.out.println(lastId);
        //mismatching header and param
        given()
                .header("X-session-ID", 1)
                .param("session_id", 2)
                .header("Content-Type", "application/json")
                .when()
                .put("/messages/" + lastId)
                .then()
                .assertThat()
                .statusCode(401);
        //good header
        given()
                .header("X-session-ID", 1)
                .header("Content-Type", "application/json")
                .when()
                .put("/messages/" + lastId)
                .then()
                .assertThat()
                .statusCode(200);
        jsonPath = given()
                .header("X-session-ID", 1)
                .header("Content-Type", "application/json")
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list2 = jsonPath.getList("read");
        assertEquals(list2.get(list2.size() - 1), true);
    }

    @Test
    public void testAddMessageNegativeBalance() {
        JSONObject jsonObject = new JSONObject().put("date", LocalDateTime.now().toString())
                .put("amount", 150).put("externalIBAN", "testIBAN").put("type", "deposit")
                .put("description", "description");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        jsonObject.remove("amount");
        jsonObject.remove("type");
        jsonObject.put("amount", 250);
        jsonObject.put("type", "withdrawal");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        JsonPath jsonPath = given()
                .header("X-session-ID", 3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list = jsonPath.getList("message");
        assertTrue(list.get(list.size() - 1).equals("Balance is negative"));
        jsonPath = given().header("X-session-ID", 3)
                .when()
                .get("/transactions")
                .then()
                .extract()
                .jsonPath();
        int id = (int) jsonPath.getList("id").get(jsonPath.getList("id").size() - 1);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + id)
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + (id - 1))
                .then()
                .assertThat()
                .statusCode(204);
    }

    @Test
    public void testAddMessageNewHigh() {
        //check if the message was added
        JSONObject jsonObject = new JSONObject().put("date", LocalDateTime.now().minusMonths(4).toString())
                .put("amount", 150).put("externalIBAN", "testIBAN").put("type", "deposit")
                .put("description", "description");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        jsonObject.remove("amount");
        jsonObject.remove("type");
        jsonObject.remove("date");
        jsonObject.put("amount", 250);
        jsonObject.put("type", "deposit");
        jsonObject.put("date", LocalDateTime.now());
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        JsonPath jsonPath = given()
                .header("X-session-ID", 3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list = jsonPath.getList("message");
        assertEquals("Balance reached new high", list.get(list.size() - 1));
        //check if the message was not read a new one will not be added
        int size = list.size();
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        jsonPath = given()
                .header("X-session-ID", 3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        list = jsonPath.getList("message");
        assertNotEquals(list.size(), size + 1);
        //check if the message was read a new one will be added
        size = list.size();
        given()
                .header("X-session-ID", 3)
                .header("Content-Type", "application/json")
                .when()
                .put("/messages/" + jsonPath.getList("id").get(jsonPath.getList("id").size() - 1))
                .then()
                .assertThat()
                .statusCode(200);
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        jsonPath = given()
                .header("X-session-ID", 3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        list = jsonPath.getList("message");
        assertEquals(list.size(), size + 1);
        jsonPath = given().header("X-session-ID", 3)
                .when()
                .get("/transactions")
                .then()
                .extract()
                .jsonPath();
        int id = (int) jsonPath.getList("id").get(jsonPath.getList("id").size() - 1);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + id)
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + (id - 1))
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + (id - 2))
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + (id - 3))
                .then()
                .assertThat()
                .statusCode(204);
    }

    @Test
    public void testSavingGoalWasReached() {
        JSONObject jsonObject = new JSONObject().put("date", LocalDateTime.now().minusMonths(1).toString())
                .put("amount", 500).put("externalIBAN", "testIBAN").put("type", "deposit")
                .put("description", "description");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);

        jsonObject = new JSONObject().put("name", "Test").put("goal", 200).put("savePerMonth", 200);
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(201);

        jsonObject = new JSONObject().put("date", LocalDateTime.now().toString())
                .put("amount", 500).put("externalIBAN", "testIBAN").put("type", "deposit")
                .put("description", "description");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);

        JsonPath jsonPath = given()
                .header("X-session-ID", 3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list = jsonPath.getList("message");
        assertEquals("Saving goal Test reached", list.get(list.size() - 1));


        jsonPath = given().header("X-session-ID", 3)
                .when()
                .get("/transactions")
                .then()
                .extract()
                .jsonPath();
        int id = (int) jsonPath.getList("id").get(jsonPath.getList("id").size() - 1);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + id)
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + (id - 1))
                .then()
                .assertThat()
                .statusCode(204);

        jsonPath = given().header("X-session-ID", 3)
                .when()
                .get("/savingGoals")
                .then()
                .extract()
                .jsonPath();
        id = (int) jsonPath.getList("id").get(jsonPath.getList("id").size() - 1);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/savingGoals/" + id)
                .then()
                .assertThat()
                .statusCode(204);
    }

    @Test
    public void testPaymentRequestNotFilled() {
        JSONObject jsonObject = new JSONObject().put("description", "test")
                .put("due_date", LocalDateTime.now().minusMonths(1))
                .put("amount", 50).put("number_of_requests", 1);
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(201);
        jsonObject = new JSONObject().put("date", LocalDateTime.now().toString())
                .put("amount", 500).put("externalIBAN", "testIBAN").put("type", "deposit")
                .put("description", "description");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);

        JsonPath jsonPath = given()
                .header("X-session-ID" ,3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list = jsonPath.getList("message");
        assertEquals("One payment request has not been filled", list.get(list.size() - 1));
        jsonPath = given().header("X-session-ID", 3)
                .when()
                .get("/transactions")
                .then()
                .extract()
                .jsonPath();
        int id = (int) jsonPath.getList("id").get(jsonPath.getList("id").size() - 1);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + id)
                .then()
                .assertThat()
                .statusCode(204);
    }

    @Test
    public void testPaymentRequestHasBeenFilled() {
        JSONObject jsonObject = new JSONObject().put("description", "test")
                .put("due_date", LocalDateTime.now().plusMonths(2).toString())
                .put("amount", 50).put("number_of_requests", 1);
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/paymentRequests")
                .then()
                .assertThat()
                .statusCode(201);
        jsonObject = new JSONObject().put("date", LocalDateTime.now().toString())
                .put("amount", 50).put("externalIBAN", "testIBAN").put("type", "deposit")
                .put("description", "description");
        given()
                .header("X-session-ID", 3)
                .body(jsonObject.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);

        JsonPath jsonPath = given()
                .header("X-session-ID" ,3)
                .when()
                .get("/messages")
                .then()
                .extract()
                .jsonPath();
        List<Object> list = jsonPath.getList("message");
        assertEquals("One payment request has been filled", list.get(list.size() - 1));
        jsonPath = given().header("X-session-ID", 3)
                .when()
                .get("/transactions")
                .then()
                .extract()
                .jsonPath();
        int id = (int) jsonPath.getList("id").get(jsonPath.getList("id").size() - 1);
        given()
                .header("X-session-ID", 3)
                .when()
                .delete("/transactions/" + id)
                .then()
                .assertThat()
                .statusCode(204);
    }
}
