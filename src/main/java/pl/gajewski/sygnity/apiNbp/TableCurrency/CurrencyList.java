package pl.gajewski.sygnity.apiNbp.TableCurrency;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CurrencyList {
    private String table;
    private String no;
    private String effectiveDate;
    private List<TableCurrency> rates = new ArrayList<TableCurrency>();
}
