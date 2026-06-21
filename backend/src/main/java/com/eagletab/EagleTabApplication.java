package com.eagletab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** EagleTab 後端服務的 Spring Boot 啟動入口。 */
@SpringBootApplication
public class EagleTabApplication {

	/** 啟動 Spring 應用程式與其中註冊的 WebSocket、終端機元件。 */
	public static void main(String[] args) {
		SpringApplication.run(EagleTabApplication.class, args);
	}

}
