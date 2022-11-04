package pl.gajewski.sygnity.apiNbp.currency;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Transcript {
    private String table;
    private String currency;
    private String code;
    private List<Rate> rates = new ArrayList<Rate>();
}
