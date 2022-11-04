package pl.gajewski.sygnity.db.entity.tableAcurrency;

import lombok.Data;
import pl.gajewski.sygnity.constant.CurrencyConstant;

import javax.persistence.*;

@Entity
@Data
@Table(name = CurrencyConstant.TABLE_A_NAME_CURRENCY)
public class CurrencyCodeTableAOB {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String code;
}
