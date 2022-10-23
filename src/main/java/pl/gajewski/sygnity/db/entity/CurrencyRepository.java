package pl.gajewski.sygnity.db.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyOB, Long> {

    CurrencyOB findByCodeAndEffectiveDate(String code, String effectiveDate);
}
