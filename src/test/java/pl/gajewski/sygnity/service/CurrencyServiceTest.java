package pl.gajewski.sygnity.service;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import pl.gajewski.sygnity.apiNbp.currency.Currency;

import pl.gajewski.sygnity.db.entity.tableAcurrency.CurrencyCodeRepository;
import pl.gajewski.sygnity.db.entity.currency.CurrencyOB;
import pl.gajewski.sygnity.db.entity.currency.CurrencyRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Autowired
    private Gson gson;
    @Mock
    private CurrencyRepository repo;

    @Mock
    private CurrencyCodeRepository currencyCodeRepository;

    @InjectMocks
    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(currencyService, "hostApiNBP", "http://api.nbp.pl/api/exchangerates/rates/A");
        ReflectionTestUtils.setField(currencyService, "listCurrencyApiNBP", "http://api.nbp.pl/api/exchangerates/tables/A/");

    }

    @Test
    void shouldConvertIsStatusOk() throws URISyntaxException, IOException, InterruptedException, ParseException {
        // given
        Currency currency = getCurrency();
        // when
        ResponseEntity<String> actual = currencyService.convert(currency);
        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    void shouldConvertIsStatusNotContent() throws URISyntaxException, IOException, InterruptedException, ParseException {
        // given
        Currency currency = getCurrency();
        currency.setConversionDate("2022-10-09");
        // when
        ResponseEntity<String> actual = currencyService.convert(currency);
        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void shouldGetCurrencyFromApiNBP() throws URISyntaxException, IOException, InterruptedException {
        // given
        // when
        HttpResponse<String> actual = currencyService.getCurrencyFromApiNBP("USD", "2022-10-10");
        // then
        assertThat(actual).isNotNull();
        assertThat(actual.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldCalculateAmountInPLN() throws URISyntaxException, IOException, InterruptedException, ParseException {
        // given
        // when
        Double actual = currencyService.calculateAmountInPLN(getCurrency(), getCurrencyOB());
        // then
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(125.8);
    }

    @Test
    void shouldGetConversionResult() throws URISyntaxException, IOException, InterruptedException, ParseException {
        // given
        // when
        Double actual = currencyService.getConversionResult(getCurrency(), 125.00, getCurrencyOB());
        // then
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(25);
    }

    @ParameterizedTest
    @MethodSource(value = "createRequest")
    void shouldValidation(Currency currency) throws URISyntaxException, IOException, InterruptedException, ParseException {
        // given
        // when
        ResponseEntity<String> actual = currencyService.convert(currency);
        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCodeValue()).isEqualTo(404);
    }

    private static Stream<Arguments> createRequest() {
        return Stream.of(
                Arguments.of(new Currency("", 20.11, "EUR", "2022-10-10")),
                Arguments.of(new Currency("USD", null, "EUR", "2022-10-10")),
                Arguments.of(new Currency("EUR", 20.12, "", "2022-10-10")),
                Arguments.of(new Currency("GBP", 20.10, "EUR", "")),
                Arguments.of(new Currency("", null, "", "")),
                Arguments.of(new Currency("EUR", null, "", "2022-10-10"))
        );
    }

    private CurrencyOB getCurrencyOB() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2022-10-10");
        CurrencyOB currencyOB = new CurrencyOB();
        currencyOB.setCode("USD");
        currencyOB.setMid(5.00);
        currencyOB.setEffectiveDate(date);
        return currencyOB;
    }

    private Currency getCurrency() {
        Currency currency = new Currency();
        currency.setSourceCurrencyCode("USD");
        currency.setAmountInSourceCurrency(25.16);
        currency.setTargetCurrencyCode("EUR");
        currency.setConversionDate("2022-10-10");
        return currency;
    }
}

