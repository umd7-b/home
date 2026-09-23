package expense_manager.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import expense_manager.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    @Query("SELECT DISTINCT e FROM Expense e " +
           "LEFT JOIN FETCH e.payers p " +
           "LEFT JOIN FETCH p.member " +
           "WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    List<Expense> findByExpenseDateBetween(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
}