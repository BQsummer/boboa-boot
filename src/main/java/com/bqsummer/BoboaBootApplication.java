package com.bqsummer;

import com.bqsummer.plugin.configplus.annotation.SnorlaxScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@MapperScan("com.bqsummer.**.mapper")
@SnorlaxScan
//@EnableWebSecurity(debug=true)
@ImportResource("classpath:applicationContext.xml")
public class BoboaBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoboaBootApplication.class, args);
    }

}
