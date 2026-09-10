package expense_manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import expense_manager.entity.ExpenseType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExpenseRequest (
    @NotNull LocalDate expenseDate,
        @NotNull ExpenseType type,
        String description,
        @NotNull @Positive BigDecimal totalAmount,
        @NotEmpty List<PayerDto> payers,
        @NotEmpty List<Long> participantIds
){
    public record PayerDto(
            @NotNull Long memberId,
            @NotNull @Positive BigDecimal amountPaid
    ) {}
}
