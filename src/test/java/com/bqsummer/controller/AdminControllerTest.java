package com.bqsummer.controller;

import com.bqsummer.BaseTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;

public class AdminControllerTest extends BaseTest {
    @Test
    public void testGetAdminInfo() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/admin/druid/stat")
                .then()
                .statusCode(200);
    }
}
