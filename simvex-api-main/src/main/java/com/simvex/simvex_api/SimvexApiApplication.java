package com.simvex.simvex_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // <--- 이 줄이 꼭 있어야 합니다!
@SpringBootApplication
public class SimvexApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimvexApiApplication.class, args);
	}

}