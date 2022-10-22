package pl.gajewski.sygnity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.gajewski.sygnity.apiNbp.Currency;
import pl.gajewski.sygnity.constant.CurrencyConstant;
import pl.gajewski.sygnity.service.CurrencyService;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping(CurrencyConstant.CURRENCY_CONVERT)
    public String convert(@RequestBody Currency currencyRequest) throws URISyntaxException, IOException, InterruptedException {
        return currencyService.convert(currencyRequest);
    }

    @GetMapping(CurrencyConstant.NOT_FOUND)
    public ResponseEntity<HttpStatus> notFound() {
        return currencyService.notFound();
    }
}
