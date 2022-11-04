package pl.gajewski.sygnity.apiNbp.currency;

import lombok.Data;

@Data
public class Rate {
    private String no;
    private String effectiveDate;
    private Double mid;
}
