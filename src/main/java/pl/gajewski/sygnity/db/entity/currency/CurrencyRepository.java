package pl.gajewski.sygnity.db.entity.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;


@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyOB, Long> {

    CurrencyOB findByCodeAndEffectiveDate(String code, Date effectiveDate);
}
