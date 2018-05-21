package nl.utwente.ing.testsuite;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertTrue;

public class ControllerTest {
	
	@BeforeClass
	public static void before() {
		RestAssured.basePath = "/api/v1";
	}

	@Test
	public void testPostSession() {
		String session =
		given().
		        contentType("application/json").
		when().
		        post("/sessions").
		then().
				contentType(ContentType.JSON).
		extract().
				response().jsonPath().getString("id");

		assertTrue(Integer.parseInt(session) > 0);

	}
}
