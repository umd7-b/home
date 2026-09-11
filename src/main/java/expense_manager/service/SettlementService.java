package expense_manager.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import expense_manager.dto.DebtDto;
import expense_manager.dto.MemberSummary;
import expense_manager.dto.SettlementRequest;
import expense_manager.entity.Member;
import expense_manager.entity.Settlement;
import expense_manager.repository.MemberRepository;
import expense_manager.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final SummaryService summaryService;

    /**
     * Tính toán ai nợ ai bao nhiêu, sử dụng thuật toán ghép nợ tối ưu (ít giao dịch nhất).
     * 
     * Thuật toán:
     * 1. Lấy balance (số dư) của mỗi người từ SummaryService
     * 2. Tách thành 2 nhóm: debtors (balance < 0, nợ) và creditors (balance > 0, dư)
     * 3. Sắp xếp theo số tiền giảm dần
     * 4. Ghép từng cặp debtor-creditor cho đến khi hết nợ
     */
    @Transactional(readOnly = true)
    public List<DebtDto> calculateDebts(LocalDate from, LocalDate to) {
        List<MemberSummary> summaries = summaryService.getSummary(from, to);
        
        // Tạo balance map đã điều chỉnh (đã bao gồm các giao dịch thanh toán)
        List<BalanceEntry> balances = new ArrayList<>();
        for (MemberSummary ms : summaries) {
            BigDecimal adjusted = ms.finalBalance();
            if (adjusted.compareTo(BigDecimal.ZERO) != 0) {
                balances.add(new BalanceEntry(ms.memberId(), ms.memberName(), ms.avatarColor(), adjusted));
            }
        }
        
        // Tách debtors (nợ, balance < 0) và creditors (dư, balance > 0)
        List<BalanceEntry> debtors = balances.stream()
                .filter(b -> b.balance.compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(b -> b.balance)) // nợ nhiều nhất trước
                .collect(Collectors.toList());
        
        List<BalanceEntry> creditors = balances.stream()
                .filter(b -> b.balance.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(b -> ((BalanceEntry) b).balance).reversed()) // dư nhiều nhất trước
                .collect(Collectors.toList());
        
        // Thuật toán ghép nợ tối ưu (greedy)
        List<DebtDto> debts = new ArrayList<>();
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            BalanceEntry debtor = debtors.get(i);
            BalanceEntry creditor = creditors.get(j);
            
            BigDecimal debtAmount = debtor.balance.abs();
            BigDecimal creditAmount = creditor.balance;
            BigDecimal transferAmount = debtAmount.min(creditAmount);
            
            // Cập nhật balance ngay lập tức để tiếp tục vòng lặp
            debtor.balance = debtor.balance.add(transferAmount);
            creditor.balance = creditor.balance.subtract(transferAmount);
            
            // Đề xuất thanh toán đúng chính xác số tiền lẻ đến từng đồng để khớp hoàn toàn với bảng Còn Lại
            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                debts.add(new DebtDto(
                    debtor.memberId, debtor.name, debtor.color,
                    creditor.memberId, creditor.name, creditor.color,
                    transferAmount
                ));
            }
            
            if (debtor.balance.compareTo(BigDecimal.ZERO) == 0) i++;
            if (creditor.balance.compareTo(BigDecimal.ZERO) == 0) j++;
        }
        
        return debts;
    }

    /**
     * Ghi nhận thanh toán (settlement).
     */
    @Transactional
    public Settlement createSettlement(SettlementRequest request) {
        Settlement settlement = new Settlement();
        settlement.setFromMember(memberRepository.getReferenceById(request.fromMemberId()));
        settlement.setToMember(memberRepository.getReferenceById(request.toMemberId()));
        settlement.setAmount(request.amount());
        settlement.setSettlementDate(LocalDate.now());
        settlement.setNote(request.note());
        settlement.setPeriodFrom(request.periodFrom());
        settlement.setPeriodTo(request.periodTo());
        return settlementRepository.save(settlement);
    }

    /**
     * Lấy lịch sử thanh toán theo kỳ.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSettlementHistory(LocalDate periodFrom, LocalDate periodTo) {
        List<Settlement> settlements = settlementRepository.findByPeriodFromAndPeriodTo(periodFrom, periodTo);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Settlement s : settlements) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", s.getId());
            row.put("fromName", s.getFromMember().getName());
            row.put("fromColor", s.getFromMember().getAvatarColor());
            row.put("toName", s.getToMember().getName());
            row.put("toColor", s.getToMember().getAvatarColor());
            row.put("amount", s.getAmount());
            row.put("date", s.getSettlementDate());
            row.put("createdAt", s.getCreatedAt());
            row.put("note", s.getNote());
            result.add(row);
        }
        
        return result;
    }

    /**
     * Hủy thanh toán.
     */
    @Transactional
    public void deleteSettlement(Long id) {
        settlementRepository.deleteById(id);
    }

    // Helper class mutable để ghép nợ
    private static class BalanceEntry {
        Long memberId;
        String name;
        String color;
        BigDecimal balance;
        
        BalanceEntry(Long memberId, String name, String color, BigDecimal balance) {
            this.memberId = memberId;
            this.name = name;
            this.color = color;
            this.balance = balance;
        }
    }
}
