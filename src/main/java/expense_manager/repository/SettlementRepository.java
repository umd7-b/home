package expense_manager.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import expense_manager.entity.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByPeriodFromAndPeriodTo(LocalDate periodFrom, LocalDate periodTo);
    
    List<Settlement> findBySettlementDateBetween(LocalDate from, LocalDate to);
}
