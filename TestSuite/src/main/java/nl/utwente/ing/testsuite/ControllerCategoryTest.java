package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class ControllerCategoryTest {

    @BeforeClass
    public static void before() {
        RestAssured.basePath = "api/v1";
    }

	@Test
	public void testGetCategories() {
		// ---- Headers ----
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", 1).
		when().
			get("categories?session_id=" + 2).
		then().
			assertThat().statusCode(401);

		// Valid header
		Response headerCategory = given().
				header("X-session-ID", 1).
				header("Content-Type", "application/JSON").
		when().
				get("/categories");

		// Checks status code
		headerCategory.
			then().
				assertThat().statusCode(200);
		// Check if IDs make sense
		for (Object id: headerCategory.jsonPath().getList("id")) {
			assertTrue((int)id >= 0);
		}

		// Check if the names are not null
		for (Object name: headerCategory.jsonPath().getList("name")) {
            assertNotNull(name);
		}

		// Do the same tests with the sessionID passed as a parameter
		Response parameterCategory = given().
				header("Content-Type", "application/JSON").
		when().
				get("/categories?session_id=" + 1);

		// Check if the ids of the responses are the same
		assertEquals(parameterCategory.jsonPath().getList("id"), headerCategory.jsonPath().getList("id"));

		// Check if the names of the responses are the same
		assertEquals(parameterCategory.jsonPath().getList("name"), headerCategory.jsonPath().getList("name"));


		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/categories").
		then().
			assertThat().statusCode(401);
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/categories").
		then().
			assertThat().statusCode(401);
	}

	@Test
	public void testPostCategory() {
		JSONObject category = new JSONObject()
											.put("name", "blah");

		// ---- Headers ----
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", 1).
			body(category.toString()).
		when().
			post("categories?session_id=" + 2).
		then().
			assertThat().statusCode(401);
		// No header
		given().
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			post("/categories").
		then().
			assertThat().statusCode(401);

		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			post("/categories").
		then().
			assertThat().statusCode(401);

		// Valid header
		Response headerCategoryResponse = given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			post("/categories");

		// Check the status code
		headerCategoryResponse.
			then().
				assertThat().statusCode(201);
		// Check the response body
		JsonPath categoryJson = headerCategoryResponse.jsonPath();
		// Check if the ID makes sense
		assertTrue(categoryJson.getInt("id") > 0);
		// Check if the name is correct
		assertEquals(categoryJson.getString("name"), category.get("name"));

		// Check if the session ID passed as a parameter works
		category.put("name", "blah blah");

		Response parameterCategoryResponse = given().
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			post("/categories?session_id=" + 1);

		// Check if the id is properly incremented
		assertEquals(parameterCategoryResponse.jsonPath().getInt("id"), headerCategoryResponse.jsonPath().getInt("id") + 1);

		// Check if the name is the same as what was posted
		assertEquals(parameterCategoryResponse.jsonPath().getString("name"),category.getString("name"));

		// ---- Invalid input ----
		// name is null
		category.remove("name");
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			post("/categories").
		then().
			assertThat().statusCode(405);

		// no body
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			post("/categories").
		then().
			assertThat().statusCode(405);
    }

	@Test
	public void testGetCategory() {
		// ---- Headers ----
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", 1).
		when().
			get("categories/" + 2 + "?session_id=" + 2).
		then().
			assertThat().statusCode(401);
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			get("/categories/" + 2).
		then().
			assertThat().statusCode(401);

		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			get("/categories/" + 2).
		then().
			assertThat().statusCode(401);

		// Valid header
		Response headerCategory = given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			get("/categories/" + 2);

		// check response code
		headerCategory.then().
			assertThat().statusCode(200);

		// check response body
		JsonPath categoryJson = headerCategory.jsonPath();
		assertEquals(categoryJson.getInt("id"), 2);
		assertNotNull(categoryJson.getString("name"));

		// Check if the GET request with the session ID passed as a parameter yields the same category
		Response parameterCategory = given().
				header("Content-Type", "application/JSON").
			when().
				get("/categories/" + 2 + "?session_id=" + 1);

		// Check if the id is the same
		assertEquals(parameterCategory.jsonPath().getInt("id"), headerCategory.jsonPath().getInt("id"));
		// Check if the name is the same
		assertEquals(parameterCategory.jsonPath().getString("name"), headerCategory.jsonPath().getString("name"));

		// ---- Invalid path ----
		// negative id
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			get("/categories/" + -1).
		then().
			assertThat().statusCode(404);
		// negative or out of session
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			// Negative or possibly out of valid index range
			get("/categories/" + 0).
		then().
			assertThat().statusCode(404);
	}

	@Test
	public void testPutCategory() {
		JSONObject category = new JSONObject()
				.put("name", "putHeaderCategoryTest");

		// ---- Headers ----
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", 1).
			body(category.toString()).
		when().
			put("categories/" + 2 + "?session_id=" + 2).
		then().
			assertThat().statusCode(401);
		// No header
		given().
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + 2).
		then().
			assertThat().statusCode(401);

		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + 2).
		then().
			assertThat().statusCode(401);

		// Valid header
		Response headerCategoryResponse = given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + 2);

		//SessionID given as parameter
		Response parameterCategoryResponse = given().
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + 2 + "?session_id=" + 1);

		// Check response codes
		headerCategoryResponse.
			then().
				assertThat().statusCode(200);

		parameterCategoryResponse.
		then().
			assertThat().statusCode(200);

		// Check response body
		JsonPath headerCategoryJson = headerCategoryResponse.jsonPath();
		JsonPath parameterCategoryJson = parameterCategoryResponse.jsonPath();

		// Check if the request works for the sessionID given in the header
		assertEquals(headerCategoryJson.getInt("id"), 2);
		assertEquals(headerCategoryJson.getString("name"), category.get("name"));


		// Check if the PUT request with the sessionID passed down as a parameter also works
		assertEquals(parameterCategoryJson.getInt("id"), 2);
		assertEquals(parameterCategoryJson.getString("name"), category.get("name"));



		// ---- Invalid path ----
		// negative id
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + -1).
		then().
			assertThat().statusCode(404);
		// negative or out of session id
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + 0).
		then().
			assertThat().statusCode(404);

		// ---- Invalid input ----
		// null name
		category.remove("name");
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(category.toString()).
		when().
			put("/categories/" + 2).
		then().
			assertThat().statusCode(405);
		// no body
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			put("/categories/" + 2).
		then().
			assertThat().statusCode(405);

	}

	@Test
	public void testDeleteCategory() {
		JsonPath categoryJson =
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
	        get("/categories").
		then().
			contentType(ContentType.JSON).
		extract().
			response().jsonPath();
		// Get the last transaction id
		int listSize = categoryJson.getList("id").size();
		String lastCategoryID = categoryJson.getList("id").get(listSize - 1).toString();
		// ---- Headers ----
		// Mismatching session IDs
		given().
			header("Content-Type", "application/JSON").
			header("X-session-ID", 1).
		when().
			delete("categories/" + 4 + "?session_id=" + 2).
		then().
			assertThat().statusCode(401);
		// No header
		given().
			header("Content-Type", "application/JSON").
		when().
			delete("/categories/" + lastCategoryID).
		then().
			assertThat().statusCode(401);

		// Invalid header
		given().
			header("X-session-ID", -1).
			header("Content-Type", "application/JSON").
		when().
			delete("/categories/" + lastCategoryID).
		then().
			assertThat().statusCode(401);

		// Valid header
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			delete("/categories/" + lastCategoryID).
		then().
			assertThat().statusCode(204);

		// Check that category was indeed deleted
		// Try another delete
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			delete("/categories/" + lastCategoryID).
		then().
			assertThat().statusCode(404);
		// Try a get request
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			get("/categories/" + lastCategoryID).
		then().
			assertThat().statusCode(404);

		// Add the category again to test the delete with the sessionID inserted as a parameter
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
			body(new JSONObject().put("name", "parameterTest").toString()).
		when().
			post("/categories").
		then().
			assertThat().statusCode(201);

		// Perform the delete
		given().
			header("Content-Type", "application/JSON").
		when().
			delete("/categories/" + lastCategoryID + "?session_id=" + 1).
		then().
			assertThat().statusCode(204);

		// Check that category was indeed deleted
		// Try another delete
		given().
			header("Content-Type", "application/JSON").
		when().
			delete("/categories/" + lastCategoryID + "?session_id=" + 1).
		then().
			assertThat().statusCode(404);
		// Try a get request
		given().
			header("X-session-ID", 1).
			header("Content-Type", "application/JSON").
		when().
			get("/categories/" + lastCategoryID).
		then().
			assertThat().statusCode(404);

	}
}
