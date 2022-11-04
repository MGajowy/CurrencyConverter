package pl.gajewski.sygnity.db.entity.tableAcurrency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyCodeRepository extends JpaRepository<CurrencyCodeTableAOB, Long> {

}
