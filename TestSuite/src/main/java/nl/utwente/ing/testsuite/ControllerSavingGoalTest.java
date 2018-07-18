package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class ControllerSavingGoalTest {

    @BeforeClass
    public static void setUp() {
        RestAssured.basePath = "api/v1";
    }

    @Test
    public void testGet() {
        //No param or header
        given()
                .when()
                .get("/savingGoals")
                .then()
                .assertThat()
                .statusCode(401);
        //Param and header are different
        given()
                .param("session_id", 1)
                .header("X-session-ID", 2)
                .when()
                .get("/savingGoals")
                .then()
                .assertThat()
                .statusCode(401);
        //Correct param
        given()
                .param("session_id",1)
                .when()
                .get("/savingGoals")
                .then()
                .assertThat()
                .statusCode(200);
        //Correct header
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/savingGoals")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    public void testPost() {
        //No minBalanceRequired selected
        JSONObject jsonObject = new JSONObject()
                .put("name", "China trip")
                .put("goal", 2500)
                .put("savePerMonth", 50);
        //No param or header
        given()
                .body(jsonObject.toString())
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(401);
        //No body
        given()
                .header("X-session-ID", 1)
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(405);
        //Correct post
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(201);
        //Invalid savingGoal
        jsonObject.remove("goal");
        jsonObject.put("goal", -1200);
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(405);
        //Missing field
        jsonObject.remove("goal");
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(405);
        //minBalanceRequired set
        jsonObject = new JSONObject()
                .put("name", "Japan trip")
                .put("goal", 10000)
                .put("savePerMonth", 100)
                .put("minBalanceRequired", 260);
        Response response = given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/savingGoals");
        response.then().assertThat().statusCode(201);
        response = given()
                .param("session_id", 1)
                .when()
                .get("/savingGoals");
        JsonPath jsonPath = response
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .response()
                .jsonPath();
        List<Object> list = jsonPath.getList("minBalanceRequired");
        assertTrue((float) list.get(list.size() - 1) == 260);
    }

    @Test
    public void testDelete() {
        JsonPath savingJson =
                given().
                        header("X-session-ID", 1).
                        header("Content-Type", "application/JSON").
                        when().
                        get("/savingGoals").
                        then().
                        contentType(ContentType.JSON).
                        extract().
                        response().jsonPath();
        // Get the last transaction id
        int listSize = savingJson.getList("id").size();
        String lastSavingGoalID = savingJson.getList("id").get(listSize - 1).toString();
        //No param or header
        given()
                .when()
                .delete("/savingGoals/" + lastSavingGoalID)
                .then()
                .assertThat()
                .statusCode(401);
        //Not valid savingGoal
        given()
                .header("X-session-ID", 1)
                .when()
                .delete("/savingGoals/-1")
                .then()
                .assertThat()
                .statusCode(404);
        //Delete correctly with header
        given()
                .header("X-session-ID", 1)
                .when()
                .delete("/savingGoals/" + lastSavingGoalID)
                .then()
                .assertThat()
                .statusCode(204);
        savingJson =
                given().
                        header("X-session-ID", 1).
                        header("Content-Type", "application/JSON").
                        when().
                        get("/savingGoals").
                        then().
                        contentType(ContentType.JSON).
                        extract().
                        response().jsonPath();
        // Get the last transaction id
        listSize = savingJson.getList("id").size();
        lastSavingGoalID = savingJson.getList("id").get(listSize - 1).toString();
        //Delete correctly with param
//        System.out.println(lastSavingGoalID);
        given()
                .param("session_id", 1)
                .when()
                .delete("/savingGoals/" + lastSavingGoalID)
                .then()
                .assertThat()
                .statusCode(204);
        //Param and header are different
        given()
                .param("session_id", 1)
                .header("X-session-ID", 2)
                .when()
                .delete("/savingGoals/" + lastSavingGoalID)
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testUpdateBalance() {
        JSONObject transaction = new JSONObject()
                .put("date", LocalDateTime.now().toString())
                .put("amount", 2500)
                .put("type", "deposit")
                .put("externalIBAN", "NL67RABO4564561230")
                .put("description", "test transaction");
        given()
                .header("X-session-ID", 5)
                .body(transaction.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        JSONObject savingGoal = new JSONObject()
                .put("name", "something")
                .put("goal", 3000)
                .put("savePerMonth", 1500);
        given()
                .header("X-session-ID", 5)
                .body(savingGoal.toString())
                .when()
                .post("/savingGoals")
                .then()
                .assertThat()
                .statusCode(201);
        transaction = new JSONObject()
                .put("date", LocalDateTime.now().plusMonths(1).toString())
                .put("amount", 5000)
                .put("type", "deposit")
                .put("externalIBAN", "NL67RABO4564561230")
                .put("description", "test transaction");
        given()
                .header("X-session-ID", 5)
                .body(transaction.toString())
                .when()
                .post("/transactions")
                .then()
                .assertThat()
                .statusCode(201);
        JsonPath jsonPath = given()
                .param("session_id", 5)
                .when()
                .get("/savingGoals")
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .response()
                .jsonPath();
        List<Object> list = jsonPath.getList("balance");
        assertTrue((float) list.get(list.size() - 1) == 1500);
        list = jsonPath.getList("id");
        jsonPath = given()
                .param("session_id", 5)
                .when()
                .get("/transactions")
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .response()
                .jsonPath();
        List<Object> list1 = jsonPath.getList("balance");
        assertTrue((float) list1.get(list1.size() - 1) == 6000);
        list1 = jsonPath.getList("id");
        given()
                .param("session_id", 5)
                .when()
                .delete("/transactions/" + list1.get(list1.size() - 1))
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .param("session_id", 5)
                .when()
                .delete("/transactions/" + list1.get(list1.size() - 2))
                .then()
                .assertThat()
                .statusCode(204);
        given()
                .param("session_id", 5)
                .when()
                .delete("/savingGoals/" + list.get(list.size() - 1))
                .then()
                .assertThat()
                .statusCode(204);
    }

}
