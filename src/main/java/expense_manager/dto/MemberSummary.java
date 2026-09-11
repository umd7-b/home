package expense_manager.dto;

import java.math.BigDecimal;

public record MemberSummary (
    Long memberId,
    String memberName,
    String avatarColor,
    BigDecimal totalPaid,
    BigDecimal totalOwed,
    BigDecimal originalBalance,
    BigDecimal totalSettled,
    BigDecimal finalBalance
){}
