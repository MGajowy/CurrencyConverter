package pl.gajewski.sygnity.apiNbp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    private String sourceCurrencyCode;
    private Integer amountInSourceCurrency;
    private String targetCurrencyCode;
    private String conversionDate;
}
