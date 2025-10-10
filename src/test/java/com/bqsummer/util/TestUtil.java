package com.bqsummer.util;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;
import static com.jayway.jsonpath.JsonPath.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;

public class TestUtil {

    public static final String username = "bqsummer@gmail.com";
    public static final String password = "1qaz@WSX";

    public static String testToken() {
        String response = given().header("Content-Type", "application/json")
                .body("{\"usernameOrEmail\":\"" + username + "\", \"password\":\"" + password + "\"}")
                .post("/api/v1/auth/login").asString();
        return JSONObject.parseObject(response).getString("accessToken");
    }

    public static String testToken(String username, String password) {
        String response = given().header("Content-Type", "application/json")
                .body("{\"usernameOrEmail\":\"" + username + "\", \"password\":\"" + password + "\"}")
                .post("/api/v1/auth/login").asString();
        return JSONObject.parseObject(response).getString("accessToken");
    }


}
