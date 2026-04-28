package br.com.fiscalizacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients // Habilita a comunicação entre microserviços
public class ReportServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportServiceApplication.class, args);
	}

}
