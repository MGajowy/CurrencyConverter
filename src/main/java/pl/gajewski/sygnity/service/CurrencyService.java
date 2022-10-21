package pl.gajewski.sygnity.service;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pl.gajewski.sygnity.apiNbp.Currency;
import pl.gajewski.sygnity.apiNbp.Rate;
import pl.gajewski.sygnity.apiNbp.Transcript;
import pl.gajewski.sygnity.db.entity.CurrencyOB;
import pl.gajewski.sygnity.db.entity.CurrencyRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Log4j2
public class CurrencyService {

    public static final String HOST = "http://api.nbp.pl/api/exchangerates/rates/A";
    private final CurrencyRepository repo;
    Double currencyValuePrice;

    public CurrencyService(CurrencyRepository repo) {
        this.repo = repo;
    }

    public Double convert(Currency currencyRequest) throws URISyntaxException, IOException, InterruptedException {
        //todo przliczyc walutę początkową na złotówki
        //todo przenieść HOST do konfiguracji
        //todo wyświetlić odpowiedz po 2 miejscach
        Double response = null;
        boolean validation = validation(currencyRequest);

        if (validation) {
            CurrencyOB currencyOB = repo.findByCodeAndEffectiveDate(currencyRequest.getTargetCurrencyCode(), currencyRequest.getConversionDate());
            if (currencyOB != null) {
                response = currencyOB.getMid() * currencyRequest.getAmountInSourceCurrency();
            } else {
                Transcript currency = getCurrencyFromApiNBP(currencyRequest);
                saveCurrencyToDB(currency);
                response = currencyValuePrice * currencyRequest.getAmountInSourceCurrency();
            }
        }
        return response;
    }

    @Transactional
    private void saveCurrencyToDB(Transcript currency) {
        CurrencyOB ob = new CurrencyOB();
        ob.setCode(currency.getCode());
        for (Rate rate : currency.getRates()) {
            ob.setEffectiveDate(rate.getEffectiveDate());
            ob.setMid(rate.getMid());
            currencyValuePrice = rate.getMid();
        }
        repo.save(ob);
    }

    private boolean validation(Currency currencyRequest) {
        if (!currencyRequest.getSourceCurrencyCode().isBlank() && !currencyRequest.getConversionDate().equals("")) {
            return currencyRequest.getAmountInSourceCurrency() != null && !currencyRequest.getSourceCurrencyCode().isBlank();
        }
        return false;
    }

    private Transcript getCurrencyFromApiNBP(Currency currencyRequest) throws URISyntaxException, IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        Transcript transcript;
        Gson gson = new Gson();
        StringBuilder stringBuilderURL = new StringBuilder(HOST);
        stringBuilderURL.append("/" + currencyRequest.getTargetCurrencyCode());
        stringBuilderURL.append("/" + currencyRequest.getConversionDate() + "/");
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(stringBuilderURL.toString()))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        transcript = gson.fromJson(getResponse.body(), Transcript.class);
        return transcript;
    }

    public ResponseEntity<HttpStatus> notFound() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
