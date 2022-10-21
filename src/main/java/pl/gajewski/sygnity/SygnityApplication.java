package pl.gajewski.sygnity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.gajewski.sygnity.service.CurrencyService;

@SpringBootApplication
public class SygnityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SygnityApplication.class, args);
	}
}
