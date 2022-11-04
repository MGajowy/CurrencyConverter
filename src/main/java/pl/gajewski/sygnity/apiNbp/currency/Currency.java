package pl.gajewski.sygnity.apiNbp.currency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    private String sourceCurrencyCode;
    private Double amountInSourceCurrency;
    private String targetCurrencyCode;
    private String conversionDate;
}
