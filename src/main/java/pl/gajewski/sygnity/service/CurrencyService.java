package pl.gajewski.sygnity.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pl.gajewski.sygnity.apiNbp.currency.Currency;
import pl.gajewski.sygnity.apiNbp.CurrencyApiNbp;
import pl.gajewski.sygnity.apiNbp.currency.Rate;
import pl.gajewski.sygnity.apiNbp.TableCurrency.CurrencyList;
import pl.gajewski.sygnity.apiNbp.TableCurrency.TableCurrency;
import pl.gajewski.sygnity.apiNbp.currency.Transcript;
import pl.gajewski.sygnity.db.entity.tableAcurrency.CurrencyCodeRepository;
import pl.gajewski.sygnity.db.entity.tableAcurrency.CurrencyCodeTableAOB;
import pl.gajewski.sygnity.db.entity.currency.CurrencyOB;
import pl.gajewski.sygnity.db.entity.currency.CurrencyRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CurrencyService implements CurrencyApiNbp {

    @Value("${currencyService.host}")
    private String hostApiNBP;

    @Value("${currencyService.hostListCurrencyTableA}")
    private String listCurrencyApiNBP;

    private Double currencyPrice;
    private final CurrencyRepository currencyRepository;
    private final CurrencyCodeRepository currencyCodeRepository;

    public CurrencyService(CurrencyRepository repo, CurrencyCodeRepository currencyCodeRepository) {
        this.currencyRepository = repo;
        this.currencyCodeRepository = currencyCodeRepository;
    }

    public ResponseEntity<String> convert(Currency currencyRequest)
            throws URISyntaxException, IOException, InterruptedException, ParseException {
        Double conversionResult;

        if (currencyRequest.getSourceCurrencyCode().equals("PLN") && currencyRequest.getTargetCurrencyCode().equals("PLN")) {
            conversionResult = currencyRequest.getAmountInSourceCurrency();
            return ResponseEntity.status(HttpStatus.OK).body(new DecimalFormat("##.##").format(conversionResult) +
                    " " + currencyRequest.getTargetCurrencyCode());
        }

        if (validation(currencyRequest)) {
            Date dateConversion = new SimpleDateFormat("yyyy-MM-dd").parse(currencyRequest.getConversionDate());
            CurrencyOB currencyOBSource = getCurrencyOB(
                    currencyRequest.getSourceCurrencyCode(),
                    dateConversion);
            CurrencyOB currencyOBTarget = getCurrencyOB(
                    currencyRequest.getTargetCurrencyCode(),
                    dateConversion);

            Double currentValueInPLN = calculateAmountInPLN(currencyRequest, currencyOBSource);
            conversionResult = getConversionResult(currencyRequest, currentValueInPLN, currencyOBTarget);
            if (currentValueInPLN != null && conversionResult != null) {
                return ResponseEntity.status(HttpStatus.OK).body(new DecimalFormat("##.##").format(conversionResult) +
                        " " + currencyRequest.getTargetCurrencyCode());
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
    }

    private CurrencyOB getCurrencyOB(String code, Date date) {
        return currencyRepository.findByCodeAndEffectiveDate(code, date);
    }

    @Override
    public Double getConversionResult(Currency currencyRequest, Double actualPriceInPLN, CurrencyOB currencyOBTarget)
            throws URISyntaxException, IOException, InterruptedException, ParseException {
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
            throws URISyntaxException, IOException, InterruptedException, ParseException {
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
    private void saveCurrencyToDB(Transcript currency) throws ParseException {
        CurrencyOB currencyOB = new CurrencyOB();
        currencyOB.setCode(currency.getCode());
        for (Rate rate : currency.getRates()) {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(rate.getEffectiveDate());
            currencyOB.setEffectiveDate(date);
            currencyOB.setMid(rate.getMid());
            currencyPrice = rate.getMid();
        }
        currencyRepository.save(currencyOB);
    }

    private boolean validation(Currency currency) throws URISyntaxException, IOException, InterruptedException {
        if (!currency.getSourceCurrencyCode().isBlank() && !currency.getTargetCurrencyCode().isBlank()) {
            return checkListCurrencyFromTable(currency.getSourceCurrencyCode(), currency.getTargetCurrencyCode()) &&
                    currency.getAmountInSourceCurrency() != null && !currency.getConversionDate().isBlank();
        }
        return false;
    }

    private boolean checkListCurrencyFromTable(String sourceCode, String targetCode) throws URISyntaxException, IOException, InterruptedException {
        List<CurrencyCodeTableAOB> listCurrencyName = currencyCodeRepository.findAll();
        List<String> list = new ArrayList<>();
        if (listCurrencyName.isEmpty()) {
            HttpResponse<String> getResponse = getListCurrencyFromApiNBP();
            Gson gson = new GsonBuilder().create();
            if (getResponse.statusCode() == 200) {
                List<String> result = new ArrayList<>();
                CurrencyList[] currencyLists = gson.fromJson(getResponse.body(), CurrencyList[].class);
                for (TableCurrency currency : Arrays.stream(currencyLists).collect(Collectors.toList()).get(0).getRates()) {
                    CurrencyCodeTableAOB ob = new CurrencyCodeTableAOB();
                    ob.setCode(currency.getCode());
                    list.add(currency.getCode());
                    currencyCodeRepository.save(ob);
                }
            }
        } else {
            list = listCurrencyName.stream().map(CurrencyCodeTableAOB::getCode).collect(Collectors.toList());
        }
        return list.contains(sourceCode) && list.contains(targetCode);
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

    public HttpResponse<String> getListCurrencyFromApiNBP() throws URISyntaxException, IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        String apiEndpoint = listCurrencyApiNBP;
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(apiEndpoint))
                .header("Accept", "application/json")
                .GET()
                .build();
        return httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
    }

}
