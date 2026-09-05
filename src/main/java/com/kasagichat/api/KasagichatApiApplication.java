package com.kasagichat.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kasagichat APIを起動するSpring Bootアプリケーション。
 */
@SpringBootApplication
@EnableJpaAuditing
public class KasagichatApiApplication {

	/**
	 * アプリケーションを起動する。
	 *
	 * @param args コマンドライン引数
	 */
	public static void main(String[] args) {
		SpringApplication.run(KasagichatApiApplication.class, args);
	}

}
