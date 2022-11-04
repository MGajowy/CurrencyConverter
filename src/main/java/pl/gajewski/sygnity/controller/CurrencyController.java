package pl.gajewski.sygnity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import pl.gajewski.sygnity.apiNbp.currency.Currency;
import pl.gajewski.sygnity.constant.CurrencyConstant;
import pl.gajewski.sygnity.service.CurrencyService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@RestController
@ResponseBody
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping(value = CurrencyConstant.CURRENCY_CONVERT)
    public ResponseEntity<String> convert(@RequestBody Currency currencyRequest) throws URISyntaxException, IOException, InterruptedException, ParseException {
        return currencyService.convert(currencyRequest);
    }
}
