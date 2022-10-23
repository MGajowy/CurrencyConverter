package pl.gajewski.sygnity.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pl.gajewski.sygnity.apiNbp.Currency;
import pl.gajewski.sygnity.apiNbp.CurrencyApiNbp;
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
public class CurrencyService implements CurrencyApiNbp {

    @Value("${currencyService.host}")
    private String hostApiNBP;
    private Double currencyPrice;
    private final CurrencyRepository repo;

    public CurrencyService(CurrencyRepository repo) {
        this.repo = repo;
    }

    public ResponseEntity<String> convert(Currency currencyRequest)
            throws URISyntaxException, IOException, InterruptedException {
        Double conversionResult;
        boolean validation = validation(currencyRequest);

        if (validation) {
            CurrencyOB currencyOBSource = repo.findByCodeAndEffectiveDate(
                    currencyRequest.getSourceCurrencyCode(),
                    currencyRequest.getConversionDate());
            CurrencyOB currencyOBTarget = repo.findByCodeAndEffectiveDate(
                    currencyRequest.getTargetCurrencyCode(),
                    currencyRequest.getConversionDate());

            Double currentValueInPLN = calculateAmountInPLN(currencyRequest, currencyOBSource);
            conversionResult = getConversionResult(currencyRequest, currentValueInPLN, currencyOBTarget);
            if (currentValueInPLN != null && conversionResult != null) {
                return ResponseEntity.status(HttpStatus.OK).body(new DecimalFormat("##.##").format(conversionResult) +
                        " " + currencyRequest.getTargetCurrencyCode());
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
    }

    @Override
    public Double getConversionResult(Currency currencyRequest, Double actualPriceInPLN, CurrencyOB currencyOBTarget)
            throws URISyntaxException, IOException, InterruptedException {
        Gson gson = new GsonBuilder().create();
        Double result = null;
        if (currencyOBTarget != null) {
            result = actualPriceInPLN / currencyOBTarget.getMid();
        } else {
            HttpResponse<String> getResponse = getCurrencyFromApiNBP(
                    currencyRequest.getTargetCurrencyCode(),
                    currencyRequest.getConversionDate());
            if (getResponse.statusCode() == 200) {
                Transcript currency = gson.fromJson(getResponse.body(), Transcript.class);
                saveCurrencyToDB(currency);
                Optional<Double> first = currency.getRates().stream().map(Rate::getMid).findFirst();
                Double targetCurrencyPrice = first.get();
                result = actualPriceInPLN / targetCurrencyPrice;
                log.info("Currency collection " + currencyRequest.getTargetCurrencyCode() + " for day " +
                        currencyRequest.getConversionDate());
            }
        }
        return result;
    }

    @Override
    public Double calculateAmountInPLN(Currency currencyRequest, CurrencyOB currencyOBSource)
            throws URISyntaxException, IOException, InterruptedException {
        Gson gson = new GsonBuilder().create();
        Double result = null;
        if (currencyOBSource != null) {
            result = currencyOBSource.getMid() * currencyRequest.getAmountInSourceCurrency();
        } else {
            HttpResponse<String> getResponse = getCurrencyFromApiNBP(
                    currencyRequest.getSourceCurrencyCode(),
                    currencyRequest.getConversionDate());
            if (getResponse.statusCode() == 200) {
                Transcript currency = gson.fromJson(getResponse.body(), Transcript.class);
                saveCurrencyToDB(currency);
                result = currencyPrice * currencyRequest.getAmountInSourceCurrency();
                log.info("Currency collection " + currencyRequest.getSourceCurrencyCode() + " for day " +
                        currencyRequest.getConversionDate());
            }
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

    @Override
    public HttpResponse<String> getCurrencyFromApiNBP(String code, String date)
            throws URISyntaxException, IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        String apiEndpoint = hostApiNBP + "/" + code +
                "/" + date + "/";
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(apiEndpoint))
                .header("Accept", "application/json")
                .GET()
                .build();
        return httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
    }
}
