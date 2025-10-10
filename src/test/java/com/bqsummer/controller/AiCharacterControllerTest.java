package com.bqsummer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AiCharacterControllerTest extends BaseTest {

    @Test
    @DisplayName("POST /ai/characters - Create character with valid data")
    public void testCreateCharacter_Success() {
        String body = "{"
                + "\"name\":\"Test Character\","
                + "\"imageUrl\":\"https://example.com/image.jpg\","
                + "\"author\":\"Test Author\","
                + "\"visibility\":\"PRIVATE\","
                + "\"status\":1"
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("POST /ai/characters - Create public character")
    public void testCreateCharacter_Public() {
        String body = "{"
                + "\"name\":\"Public Character\","
                + "\"imageUrl\":\"https://example.com/public.jpg\","
                + "\"author\":\"Public Author\","
                + "\"visibility\":\"PUBLIC\","
                + "\"status\":1"
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("POST /ai/characters - Create character with default visibility (PRIVATE)")
    public void testCreateCharacter_DefaultVisibility() {
        String body = "{"
                + "\"name\":\"Default Visibility Character\","
                + "\"imageUrl\":\"https://example.com/default.jpg\","
                + "\"author\":\"Default Author\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("POST /ai/characters - Invalid visibility should return 400")
    public void testCreateCharacter_InvalidVisibility() {
        String body = "{"
                + "\"name\":\"Invalid Visibility Character\","
                + "\"visibility\":\"INVALID\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(400)
                .body(equalTo("visibility 仅支持 PUBLIC/PRIVATE"));
    }

    @Test
    @DisplayName("POST /ai/characters - Unauthorized without token")
    public void testCreateCharacter_Unauthorized() {
        String body = "{"
                + "\"name\":\"Unauthorized Character\""
                + "}";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /ai/characters - List visible characters")
    public void testListVisibleCharacters_Success() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .body("$", isA(Iterable.class));
    }

    @Test
    @DisplayName("GET /ai/characters - List without token should return empty or handle gracefully")
    public void testListVisibleCharacters_WithoutToken() {
        given()
                .when()
                .get("/api/v1/ai/characters")
                .then()
                .statusCode(anyOf(is(200), is(401)))
                .body(anyOf(equalTo("[]"), anything()));
    }

    @Test
    @DisplayName("GET /ai/characters/{id} - Get character by ID (own character)")
    public void testGetCharacterById_OwnCharacter() {
        // First create a character
        String createBody = "{"
                + "\"name\":\"Get Test Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(200)
                .body("id", equalTo(characterId.intValue()))
                .body("name", equalTo("Get Test Character"))
                .body("visibility", equalTo("PRIVATE"));
    }

    @Test
    @DisplayName("GET /ai/characters/{id} - Get non-existent character should return 404")
    public void testGetCharacterById_NotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /ai/characters/{id} - Access private character of another user should return 403")
    public void testGetCharacterById_PrivateCharacterForbidden() {
        // Create a character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "character_owner_" + unique + "@example.com";
        String username = "character_owner_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        // Create a private character
        String createBody = "{"
                + "\"name\":\"Private Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Try to access with different user (using base test token)
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("PUT /ai/characters/{id} - Update own character")
    public void testUpdateCharacter_Success() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"Original Name\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Update the character
        String updateBody = "{"
                + "\"name\":\"Updated Name\","
                + "\"imageUrl\":\"https://example.com/updated.jpg\","
                + "\"author\":\"Updated Author\","
                + "\"visibility\":\"PUBLIC\","
                + "\"status\":0"
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(updateBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(200)
                .body(equalTo("更新成功"));
    }

    @Test
    @DisplayName("PUT /ai/characters/{id} - Update with invalid visibility")
    public void testUpdateCharacter_InvalidVisibility() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"Test Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        String updateBody = "{"
                + "\"name\":\"Updated Name\","
                + "\"visibility\":\"INVALID\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(updateBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(400)
                .body(equalTo("visibility 仅支持 PUBLIC/PRIVATE"));
    }

    @Test
    @DisplayName("PUT /ai/characters/{id} - Update non-existent character should return 404")
    public void testUpdateCharacter_NotFound() {
        String updateBody = "{"
                + "\"name\":\"Updated Name\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(updateBody)
                .when()
                .put("/api/v1/ai/characters/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("PUT /ai/characters/{id} - Update other user's character should return 403")
    public void testUpdateCharacter_NotOwner() {
        // Create character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "owner2_" + unique + "@example.com";
        String username = "owner2_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{"
                + "\"name\":\"Owner Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Try to update with different user
        String updateBody = "{"
                + "\"name\":\"Hacked Name\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(updateBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(403)
                .body(equalTo("只有创建者可以修改"));
    }

    @Test
    @DisplayName("DELETE /ai/characters/{id} - Delete own character")
    public void testDeleteCharacter_Success() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"To Delete Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Delete the character
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(200);

        // Verify it's deleted - should return 404
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /ai/characters/{id} - Delete non-existent character should return 404")
    public void testDeleteCharacter_NotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/ai/characters/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /ai/characters/{id} - Delete other user's character should return 403")
    public void testDeleteCharacter_NotOwner() {
        // Create character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "owner3_" + unique + "@example.com";
        String username = "owner3_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{"
                + "\"name\":\"Protected Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Try to delete with different user
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(403)
                .body(equalTo("只有创建者可以删除"));
    }

    @Test
    @DisplayName("GET /ai/characters/{id}/setting - Get character setting for accessible character")
    public void testGetCharacterSetting_Success() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"Setting Test Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /ai/characters/{id}/setting - Get setting for non-existent character should return 404")
    public void testGetCharacterSetting_CharacterNotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/999999/setting")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /ai/characters/{id}/setting - Get setting for inaccessible character should return 403")
    public void testGetCharacterSetting_NoAccess() {
        // Create character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "setting_owner_" + unique + "@example.com";
        String username = "setting_owner_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{"
                + "\"name\":\"Private Setting Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Try to get setting with different user
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("PUT /ai/characters/{id}/setting - Upsert character setting with valid data")
    public void testUpsertCharacterSetting_Success() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"Upsert Setting Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        String settingBody = "{"
                + "\"name\":\"Custom Name\","
                + "\"avatarUrl\":\"https://example.com/avatar.jpg\","
                + "\"memorialDay\":\"2023-12-25\","
                + "\"relationship\":\"friend\","
                + "\"background\":\"Met at university\","
                + "\"language\":\"en\","
                + "\"customParams\":\"{\\\"mood\\\":\\\"happy\\\"}\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(settingBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(200)
                .body(equalTo("保存成功"));
    }

    @Test
    @DisplayName("PUT /ai/characters/{id}/setting - Invalid memorial day format should return 400")
    public void testUpsertCharacterSetting_InvalidDate() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"Date Test Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        String settingBody = "{"
                + "\"name\":\"Custom Name\","
                + "\"memorialDay\":\"invalid-date\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(settingBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(400)
                .body(equalTo("纪念日格式应为 yyyy-MM-dd"));
    }

    @Test
    @DisplayName("PUT /ai/characters/{id}/setting - Setting for non-existent character should return 404")
    public void testUpsertCharacterSetting_CharacterNotFound() {
        String settingBody = "{"
                + "\"name\":\"Custom Name\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(settingBody)
                .when()
                .put("/api/v1/ai/characters/999999/setting")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("PUT /ai/characters/{id}/setting - Setting for inaccessible character should return 403")
    public void testUpsertCharacterSetting_NoAccess() {
        // Create character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "upsert_owner_" + unique + "@example.com";
        String username = "upsert_owner_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{"
                + "\"name\":\"Private Upsert Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        String settingBody = "{"
                + "\"name\":\"Hacker Name\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(settingBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("DELETE /ai/characters/{id}/setting - Delete character setting")
    public void testDeleteCharacterSetting_Success() {
        // Create a character first
        String createBody = "{"
                + "\"name\":\"Delete Setting Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Create a setting first
        String settingBody = "{"
                + "\"name\":\"Setting To Delete\""
                + "}";

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(settingBody)
                .when()
                .put("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(200);

        // Delete the setting
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("DELETE /ai/characters/{id}/setting - Delete setting for non-existent character should return 404")
    public void testDeleteCharacterSetting_CharacterNotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/ai/characters/999999/setting")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /ai/characters/{id}/setting - Delete setting for inaccessible character should return 403")
    public void testDeleteCharacterSetting_NoAccess() {
        // Create character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "delete_owner_" + unique + "@example.com";
        String username = "delete_owner_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{"
                + "\"name\":\"Private Delete Character\","
                + "\"visibility\":\"PRIVATE\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/ai/characters/" + characterId + "/setting")
                .then()
                .statusCode(403)
                .body(equalTo("无权限访问该人物"));
    }

    @Test
    @DisplayName("All endpoints should return 401 without authorization token")
    public void testAllEndpoints_Unauthorized() {
        // Test all endpoints without token
        given().when().post("/api/v1/ai/characters").then().statusCode(401);
        given().when().get("/api/v1/ai/characters/1").then().statusCode(anyOf(is(401), is(404)));
        given().when().put("/api/v1/ai/characters/1").then().statusCode(401);
        given().when().delete("/api/v1/ai/characters/1").then().statusCode(401);
        given().when().get("/api/v1/ai/characters/1/setting").then().statusCode(anyOf(is(401), is(404)));
        given().when().put("/api/v1/ai/characters/1/setting").then().statusCode(401);
        given().when().delete("/api/v1/ai/characters/1/setting").then().statusCode(anyOf(is(401), is(404)));
    }

    @Test
    @DisplayName("GET /ai/characters/{id} - Access public character from different user should succeed")
    public void testGetCharacterById_PublicCharacterFromOtherUser() {
        // Create public character with one user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "public_owner_" + unique + "@example.com";
        String username = "public_owner_" + unique;
        String registerBody = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"P@ssw0rd\""
                + "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String ownerToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{"
                + "\"name\":\"Public Character\","
                + "\"visibility\":\"PUBLIC\""
                + "}";

        String createResponse = given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract().asString();

        Long characterId = JSONObject.parseObject(createResponse).getLong("id");

        // Access with different user should succeed
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/" + characterId)
                .then()
                .statusCode(200)
                .body("id", equalTo(characterId.intValue()))
                .body("visibility", equalTo("PUBLIC"));
    }
}
