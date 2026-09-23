package expense_manager.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import expense_manager.entity.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    
    @Query("SELECT s FROM Settlement s JOIN FETCH s.fromMember JOIN FETCH s.toMember " +
           "WHERE s.periodFrom = :periodFrom AND s.periodTo = :periodTo " +
           "ORDER BY s.createdAt DESC")
    List<Settlement> findByPeriodFromAndPeriodTo(@Param("periodFrom") LocalDate periodFrom, 
                                                 @Param("periodTo") LocalDate periodTo);
    
    List<Settlement> findBySettlementDateBetween(LocalDate from, LocalDate to);
}
