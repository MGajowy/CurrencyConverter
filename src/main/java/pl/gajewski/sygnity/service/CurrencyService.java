package pl.gajewski.sygnity.service;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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
import java.text.DecimalFormat;
import java.util.Optional;

@Service
@Log4j2
public class CurrencyService {

    @Value("${currencyService.host}")
    private String hostApiNBP;
    private Double currencyPrice;
    private final CurrencyRepository repo;

    public CurrencyService(CurrencyRepository repo) {
        this.repo = repo;
    }

    public String convert(Currency currencyRequest) throws URISyntaxException, IOException, InterruptedException {
        Double response = null;
        Double currentValueInPLN = null;
        boolean validation = validation(currencyRequest);

            //todo obsłużyć 404
        if (validation) {
            CurrencyOB currencyOBSource = repo.findByCodeAndEffectiveDate(currencyRequest.getSourceCurrencyCode(), currencyRequest.getConversionDate());
            CurrencyOB currencyOBTarget = repo.findByCodeAndEffectiveDate(currencyRequest.getTargetCurrencyCode(), currencyRequest.getConversionDate());

            currentValueInPLN = calculateAmountInPLN(currencyRequest, currencyOBSource);
            response = getConversionResult(currencyRequest, currentValueInPLN, currencyOBTarget);
        }
     return new DecimalFormat("##.##").format(response);
    }

    private Double getConversionResult(Currency currencyRequest, Double actualPriceInPLN, CurrencyOB currencyOBTarget) throws URISyntaxException, IOException, InterruptedException {
        Double response;
        Double targetCurrencyPrice;
        if (currencyOBTarget != null) {
            response = actualPriceInPLN / currencyOBTarget.getMid();
        } else {
            Transcript currency = getCurrencyFromApiNBP(currencyRequest.getTargetCurrencyCode(), currencyRequest.getConversionDate());
            saveCurrencyToDB(currency);
            Optional<Double> first = currency.getRates().stream().map(Rate::getMid).findFirst();
            targetCurrencyPrice = first.get();
            response = actualPriceInPLN / targetCurrencyPrice;
            log.info("Currency collection " + currencyRequest.getTargetCurrencyCode() + " for day " + currencyRequest.getConversionDate());
        }
        return response;
    }

    private Double calculateAmountInPLN(Currency currencyRequest, CurrencyOB currencyOBSource) throws URISyntaxException, IOException, InterruptedException {
        Double result;
        if (currencyOBSource != null) {
            result = currencyOBSource.getMid() * currencyRequest.getAmountInSourceCurrency();
        } else {
            Transcript currency = getCurrencyFromApiNBP(currencyRequest.getSourceCurrencyCode(), currencyRequest.getConversionDate());
            saveCurrencyToDB(currency);
            result = currencyPrice * currencyRequest.getAmountInSourceCurrency();
            log.info("Currency collection " + currencyRequest.getSourceCurrencyCode() + " for day " + currencyRequest.getConversionDate());
        }
        return result;
    }

    @Transactional
    private void saveCurrencyToDB(Transcript currency) {
        CurrencyOB ob = new CurrencyOB();
        ob.setCode(currency.getCode());
        for (Rate rate : currency.getRates()) {
            ob.setEffectiveDate(rate.getEffectiveDate());
            ob.setMid(rate.getMid());
            currencyPrice = rate.getMid();
        }
        repo.save(ob);
    }

    private boolean validation(Currency currencyRequest) {
        if (!currencyRequest.getSourceCurrencyCode().isBlank() && !currencyRequest.getConversionDate().equals("")) {
            return currencyRequest.getAmountInSourceCurrency() != null && !currencyRequest.getSourceCurrencyCode().isBlank();
        }
        return false;
    }

    private Transcript getCurrencyFromApiNBP(String code, String date) throws URISyntaxException, IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        Gson gson = new Gson();
        StringBuilder stringBuilderURL = new StringBuilder(hostApiNBP);
        stringBuilderURL.append("/" + code);
        stringBuilderURL.append("/" + date + "/");
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(stringBuilderURL.toString()))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        return gson.fromJson(getResponse.body(), Transcript.class);
    }

    public ResponseEntity<HttpStatus> notFound() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
