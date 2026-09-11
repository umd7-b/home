package expense_manager.dto;

import java.math.BigDecimal;

public record DebtDto(
    Long fromMemberId,
    String fromName,
    String fromColor,
    Long toMemberId,
    String toName,
    String toColor,
    BigDecimal amount
) {}
