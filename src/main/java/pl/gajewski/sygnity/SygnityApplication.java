package pl.gajewski.sygnity;

import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import pl.gajewski.sygnity.service.CurrencyService;

@SpringBootApplication
public class SygnityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SygnityApplication.class, args);
	}

	@Bean
	public Gson gson() {
		return new Gson();
	}
}
