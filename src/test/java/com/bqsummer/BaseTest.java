package com.bqsummer;

import com.bqsummer.util.TestUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public class BaseTest {

    public static String token;

    @BeforeAll
    public static void setup() {
        token = TestUtil.testToken();
        log.info("Test token: {}", token);
    }
}
