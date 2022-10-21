package pl.gajewski.sygnity.db.entity;

import lombok.Data;

import pl.gajewski.sygnity.constant.CurrencyConstant;

import javax.persistence.*;

@Entity
@Data
@Table(name = CurrencyConstant.TABLE_CURRENCY)
public class CurrencyOB {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String code;

    @Column(nullable = false, length = 10)
    private Double mid;

    @Column(nullable = false, length = 36)
    private String effectiveDate;
}
