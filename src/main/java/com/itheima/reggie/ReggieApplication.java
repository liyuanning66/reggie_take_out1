package com.itheima.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @ServletComponentScan才会扫描跟servlet有关的注解，如@WebFilter
 * @EnableTransactionManagement开启事务注解的支持
 */
@SpringBootApplication
@ServletComponentScan
@Slf4j
@EnableTransactionManagement
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class, args);
        log.info("项目启动成功");
        System.out.println("我喜欢吃饭");
    }
}
