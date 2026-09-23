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
     * Tính toán ai nợ ai bao nhiêu, sử dụng thuật toán tối ưu số giao dịch ít nhất.
     * 
     * Thuật toán Min-Transactions:
     * 1. Lấy finalBalance (số dư cuối) của mỗi người từ SummaryService
     * 2. Tách thành 2 nhóm: debtors (balance < 0, nợ) và creditors (balance > 0, dư)
     * 3. Ưu tiên tìm các cặp debtor-creditor có số tiền bằng nhau (exact match) 
     *    → Mỗi cặp exact match giảm được 1 giao dịch so với greedy
     * 4. Phần còn lại dùng greedy ghép cặp (nợ nhiều nhất ghép với dư nhiều nhất)
     * 
     * Ví dụ: A nợ 100k, B nợ 200k, C dư 100k, D dư 200k
     * - Greedy: A→D 100k, B→D 100k, B→C 100k (3 giao dịch)
     * - Min-Tx: A→C 100k (exact), B→D 200k (exact) (2 giao dịch) ← tối ưu hơn
     */
    @Transactional(readOnly = true)
    public List<DebtDto> calculateDebts(LocalDate from, LocalDate to) {
        List<MemberSummary> summaries = summaryService.getSummary(from, to);
        
        // Tạo danh sách balance đã điều chỉnh (đã bao gồm các giao dịch thanh toán trước đó)
        List<BalanceEntry> debtors = new ArrayList<>();
        List<BalanceEntry> creditors = new ArrayList<>();
        
        for (MemberSummary ms : summaries) {
            BigDecimal adjusted = ms.finalBalance();
            if (adjusted.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new BalanceEntry(ms.memberId(), ms.memberName(), ms.avatarColor(), adjusted));
            } else if (adjusted.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new BalanceEntry(ms.memberId(), ms.memberName(), ms.avatarColor(), adjusted));
            }
        }
        
        List<DebtDto> debts = new ArrayList<>();
        
        // === Bước 1: Tìm các cặp exact match (nợ == dư) để giảm số giao dịch ===
        for (int i = 0; i < debtors.size(); i++) {
            BalanceEntry debtor = debtors.get(i);
            if (debtor.balance.compareTo(BigDecimal.ZERO) == 0) continue;
            
            for (int j = 0; j < creditors.size(); j++) {
                BalanceEntry creditor = creditors.get(j);
                if (creditor.balance.compareTo(BigDecimal.ZERO) == 0) continue;
                
                // Nếu số nợ == số dư → ghép thành 1 giao dịch duy nhất, triệt tiêu cả 2
                if (debtor.balance.abs().compareTo(creditor.balance) == 0) {
                    debts.add(new DebtDto(
                        debtor.memberId, debtor.name, debtor.color,
                        creditor.memberId, creditor.name, creditor.color,
                        debtor.balance.abs()
                    ));
                    debtor.balance = BigDecimal.ZERO;
                    creditor.balance = BigDecimal.ZERO;
                    break;
                }
            }
        }
        
        // === Bước 2: Sắp xếp phần còn lại và ghép greedy ===
        // Lọc bỏ các entry đã triệt tiêu ở bước 1
        List<BalanceEntry> remainingDebtors = debtors.stream()
                .filter(b -> b.balance.compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(b -> b.balance)) // nợ nhiều nhất trước (giá trị âm nhỏ nhất)
                .collect(Collectors.toList());
        
        List<BalanceEntry> remainingCreditors = creditors.stream()
                .filter(b -> b.balance.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(b -> ((BalanceEntry) b).balance).reversed()) // dư nhiều nhất trước
                .collect(Collectors.toList());
        
        int i = 0, j = 0;
        while (i < remainingDebtors.size() && j < remainingCreditors.size()) {
            BalanceEntry debtor = remainingDebtors.get(i);
            BalanceEntry creditor = remainingCreditors.get(j);
            
            BigDecimal debtAmount = debtor.balance.abs();
            BigDecimal creditAmount = creditor.balance;
            BigDecimal transferAmount = debtAmount.min(creditAmount);
            
            debtor.balance = debtor.balance.add(transferAmount);
            creditor.balance = creditor.balance.subtract(transferAmount);
            
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
        
        // Sắp xếp kết quả: giao dịch lớn nhất lên trước
        debts.sort(Comparator.comparing(DebtDto::amount).reversed());
        
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
        settlement.setSettlementDate(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
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
