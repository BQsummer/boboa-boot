你现在是一名资深的 Java 测试开发工程师，请帮我生成基于 RestAssured 的单元测试代码, 针对UserController及这个controller调用的其他方法, 不能改其他代码。
要求如下：
- 基本要求
1. 使用 JUnit 5 作为测试框架。
2. 使用 RestAssured 来进行 HTTP 接口请求和断言。
3. 测试代码必须结构清晰，类名与方法名符合规范（如 xxxControllerTest、shouldReturnXXXWhenYYY）。
4. 每个测试方法要有简要的注释，说明测试目的。
5. 使用默认的 RestAssured 的 baseURI、basePath、port 等公共配置, 不需要手动指定。
6. 所有断言必须严格，使用 DbAssert 和 DbAssertions 进行数据库相关的断言; 如果需要: 以数据库的结果为判断单测是否成功的依据。
7. 不要使用 MockMvc/WebMvcTest/SpringbootTest 进行测试, 只使用 RestAssured。

- 分支覆盖要求
1. 覆盖 所有逻辑分支：包括正常情况（200）、参数错误（400）、未授权（401/403）、资源未找到（404）、服务器错误（500）等。
2. 对于需要路径参数或查询参数的 API，要覆盖 合法参数 和 非法参数 两种情况。
3. 如果 API 依赖请求体（POST/PUT），要测试 完整数据 和 缺失必填字段 两种情况。
4. 如果接口有分页或过滤条件，要覆盖 边界情况（如 page=0, page=-1, size=0, size超大）。

-RestAssured 基本使用方法（请在代码里体现）

GET 请求：
given()
.queryParam("id", 1)
.when()
.get("/api/v1/user")
.then()
.statusCode(200)
.body("name", equalTo("Tom"));

POST 请求：
given()
.header("Content-Type", "application/json")
.body("{\"name\":\"Tom\", \"age\":25}")
.when()
.post("/api/v1/user")
.then()
.statusCode(201)
.body("id", notNullValue());

断言包含字段：
.then()
.body("errors", hasSize(greaterThan(0)));


路径参数：
given()
.pathParam("id", 1)
.when()
.get("/api/v1/user/{id}")
.then()
.statusCode(200);

- 其他要求

1. 生成的测试代码要包含 正向用例 和 反向用例。
2. 每个分支都必须有对应的单测方法。
3. 在需要的地方请使用 静态导入（如 import static io.restassured.RestAssured.*; 和 import static org.hamcrest.Matchers.*;）。
4. 未带token的请求, 响应的状态码为401, 且没有消息体