package com.nanhua.spring_ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.nanhua.spring_ai.mapper")
@SpringBootApplication
public class SpringAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiApplication.class, args);
	}

}
