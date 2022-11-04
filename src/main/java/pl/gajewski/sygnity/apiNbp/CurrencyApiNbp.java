package pl.gajewski.sygnity.apiNbp;

import pl.gajewski.sygnity.apiNbp.currency.Currency;
import pl.gajewski.sygnity.db.entity.currency.CurrencyOB;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.text.ParseException;

public interface CurrencyApiNbp {
    HttpResponse<String> getCurrencyFromApiNBP(String code, String date) throws
            URISyntaxException,
            IOException,
            InterruptedException;

    Double getConversionResult(Currency currencyRequest, Double actualPriceInPLN, CurrencyOB currencyOBTarget) throws
            URISyntaxException,
            IOException,
            InterruptedException, ParseException;

    Double calculateAmountInPLN(Currency currencyRequest, CurrencyOB currencyOBSource) throws
            URISyntaxException,
            IOException,
            InterruptedException, ParseException;
}
