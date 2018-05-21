package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class ControllerCategoryRuleTest {

    @BeforeClass
    public static void setUp() {
        RestAssured.basePath = "api/v1";
    }

    @Test
    public void testGetCategoryRules() {
        assertNotNull(given().param("session_id", 1).when().get("/categoryRules"));
        given()
                .param("session_id", 1)
                .when()
                .get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(200);
        Response categoryRule = given()
                .param("session_id", 1)
                .when()
                .get("/categoryRules");
        for (Object id: categoryRule.jsonPath().getList("id")) {
            assertTrue((int) id > 0);
        }

        //check if the other fields are not null
        assertNotNull(categoryRule.jsonPath().getList("description"));
        assertNotNull(categoryRule.jsonPath().getList("IBAN"));
        assertNotNull(categoryRule.jsonPath().getList("type"));
        assertNotNull(categoryRule.jsonPath().getList("categoryId"));
        assertNotNull(categoryRule.jsonPath().getList("applyOnHistory"));

        //no param or header
        get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(401);

        //invalid param
        given()
                .param("session_id", -2)
                .when()
                .get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(401);

        //valid header
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(200);

        //mismatching header and param
        given()
                .header("X-session-ID", 1)
                .param("session_id", 2)
                .when()
                .get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testPostCategoryRules() {
        //correct header
        JSONObject categoryRule = new JSONObject()
                .put("description", "fight club")
                .put("IBAN", "NL67INGB5879587958")
                .put("type", "withdrawal")
                .put("categoryId", 3);
        Response response = given()
                    .body(categoryRule.toString())
                    .header("X-session-ID", 1)
                    .header("Content-Type", "application/json")
                    .when()
                    .post("/categoryRules");
        response.then().assertThat().statusCode(201);

        //no header and no param
        response = given()
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules");
        response.then().assertThat().statusCode(401);

        //no body
        response = given()
                .header("X-session-ID", 1)
                .when()
                .post("/categoryRules");
        response.then().assertThat().statusCode(405);

        //with param
        given()
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules?session_id=1")
                .then()
                .assertThat()
                .statusCode(201);

        //check if it was really added
        response = given()
                .header("X-session-ID", 1)
                .when()
                .get("/categoryRules");
        assertTrue(response.jsonPath().getList("description").contains("fight club"));

        //param and header mismatching
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules?session_id=2")
                .then()
                .assertThat()
                .statusCode(401);
        //no description
        categoryRule.remove("description");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //no IBAN
        categoryRule.put("description", "aosdifsnmf");
        categoryRule.remove("IBAN");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //no categoryID
        categoryRule.put("IBAN", "NL89INGB9889898989");
        categoryRule.remove("categoryId");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //invalid type
        categoryRule.put("categoryId", 3);
        categoryRule.put("type", "invalid type");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //check if category rule is correctly applied to transaction
        JSONObject jsonObject = new JSONObject().put("description", "")
                .put("IBAN", "NL89INGB0258025802")
                .put("type", "deposit")
                .put("categoryId", 1);
        given()
                .header("X-session-ID", 1)
                .body(jsonObject.toString())
                .when()
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(201);
        //Transaction 2 should have been assigned with a new category with id 1
        JsonPath jsonPath = given()
                .param("session_id", 1)
                .when()
                .get("/transactions/2")
                .then()
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();

        Map<Object, Object> category = jsonPath.getMap("category");
        assertEquals(1, category.get("id"));
        //Now we check if the category is applied on history
        jsonPath = given()
                .param("session_id", 1)
                .when()
                .get("/categoryRules")
                .jsonPath();
        assertTrue(jsonPath.getList("applyOnHistory").contains(true) && jsonPath.getList("id").contains(5));
    }

    @Test
    public void testGetCategoryRule() {
        //only param given
        given()
                .param("session_id", 1)
                .when()
                .get("/categoryRules/1")
                .then()
                .assertThat()
                .statusCode(200);
        //no param or header
        get("/categoryRules/1")
                .then()
                .assertThat()
                .statusCode(401);
        //only header given
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/categoryRules/1")
                .then()
                .assertThat()
                .statusCode(200);
        //param and header mismatching
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/categoryRules/1?session_id=2")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testPutCategoryRule() {
        JSONObject categoryRule = new JSONObject()
                        .put("description", "hakuna matata")
                        .put("IBAN" ,"NL99INGB1598159815")
                        .put("type", "withdrawal")
                        .put("categoryId", 2);
        //correct test
        Response response = given()
                .header("X-session-ID", 2)
                .header("Content-Type", "application/json")
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules/1");
        response.then().assertThat().statusCode(200);
        response = given().param("session_id", 2).get("/categoryRules");
        assertTrue(response.jsonPath().getList("description").contains("hakuna matata"));
        //no header
        response = given()
                .header("Content-Type", "application/json")
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules/1");
        response.then().assertThat().statusCode(401);

        //no body
        response = given()
                .header("X-session-ID", 2)
                .header("Content-Type", "application/json")
                .when()
                .put("/categoryRules/1");
        response.then().assertThat().statusCode(405);

        //id is not part of the session
        categoryRule = new JSONObject()
                .put("description", "timon & pumba")
                .put("IBAN", "NL78INGB0000000000")
                .put("categoryId", 1)
                .put("type", "deposit");
        response = given()
                .header("X-session-ID", 1)
                .header("Content-Type", "application/json")
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules/0");
        response.then().assertThat().statusCode(200);
        response = given().param("session_id", 1).get("/categoryRules");
        assertFalse(response.jsonPath().getList("description").contains("timon & pumba"));
        //no description
        categoryRule.remove("description");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //no IBAN
        categoryRule.put("description", "aosdifsnmf");
        categoryRule.remove("IBAN");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //no categoryID
        categoryRule.put("IBAN", "NL89INGB9889898989");
        categoryRule.remove("categoryId");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
        //invalid type
        categoryRule.put("categoryId", 3);
        categoryRule.put("type", "invalid type");
        given()
                .header("X-session-ID", 1)
                .body(categoryRule.toString())
                .when()
                .put("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
    }

    @Test
    public void testDeleteCategoryRules() {
        JsonPath categoryJson =
                given().
                        header("X-session-ID", 1).
                        header("Content-Type", "application/JSON").
                        when().
                        get("/categoryRules").
                        then().
                        contentType(ContentType.JSON).
                        extract().
                        response().jsonPath();
        // Get the last transaction id
        int listSize = categoryJson.getList("id").size();
        String lastCategoryRuleID = categoryJson.getList("id").get(listSize - 1).toString();
        //only param
        Response response = given()
                .param("session_id", 1)
                .when()
                .delete("/categoryRules/" + Integer.parseInt(lastCategoryRuleID));
        response.then().assertThat().statusCode(204);

        //try to delete again
        response = given()
                .param("session_id", 1)
                .when()
                .delete("/categoryRules/" + Integer.parseInt(lastCategoryRuleID));
        response.then().assertThat().statusCode(404);

        //no param and header
        given()
                .when()
                .delete("/categoryRules/3")
                .then()
                .assertThat()
                .statusCode(401);

        categoryJson =
                given().
                        header("X-session-ID", 1).
                        header("Content-Type", "application/JSON").
                        when().
                        get("/categoryRules").
                        then().
                        contentType(ContentType.JSON).
                        extract().
                        response().jsonPath();
        // Get the last transaction id
        listSize = categoryJson.getList("id").size();
        lastCategoryRuleID = categoryJson.getList("id").get(listSize - 1).toString();

        //no param but with header
        given()
                .header("X-session-ID", 1)
                .when()
                .delete("/categoryRules/" + Integer.parseInt(lastCategoryRuleID))
                .then()
                .assertThat()
                .statusCode(204);

        //check if it was really deleted
        given()
                .header("X-session-ID", 1)
                .when()
                .get("/categoryRules/" + Integer.parseInt(lastCategoryRuleID))
                .then()
                .assertThat()
                .statusCode(404);
        //param and header given but are different
        given()
                .header("X-session-ID", 1)
                .param("session_id", 2)
                .when()
                .delete("/categoryRules/" + Integer.parseInt(lastCategoryRuleID))
                .then()
                .assertThat()
                .statusCode(401);

        //category rule not found
        given()
                .param("session_id", 1)
                .when()
                .delete("/categoryRules/-1")
                .then()
                .assertThat()
                .statusCode(404);
    }

}
