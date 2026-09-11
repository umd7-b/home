package expense_manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SettlementRequest(
    @NotNull Long fromMemberId,
    @NotNull Long toMemberId,
    @NotNull @Positive BigDecimal amount,
    String note,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo
) {}
