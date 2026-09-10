package expense_manager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import expense_manager.dto.MemberSummary;
import expense_manager.entity.Expense;
import expense_manager.entity.ExpenseType;
import expense_manager.entity.Member;
import expense_manager.repository.ExpenseRepository;
import expense_manager.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {
    private final ExpenseRepository expenseRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<MemberSummary> getSummary(LocalDate from, LocalDate to) {
        List<Member> activeMembers = memberRepository.findAll().stream()
                .filter(Member::isActive)
                .toList();
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(from, to);

        Map<Long, BigDecimal> paidMap = new HashMap<>();
        Map<Long, BigDecimal> owedMap = new HashMap<>();
        
        for (Member m : activeMembers) {
            paidMap.put(m.getId(), BigDecimal.ZERO);
            owedMap.put(m.getId(), BigDecimal.ZERO);
        }

        for (Expense expense : expenses) {
            expense.getPayers().forEach(payer -> {
                Long pId = payer.getMember().getId();
                paidMap.put(pId, paidMap.getOrDefault(pId, BigDecimal.ZERO).add(payer.getAmountPaid()));
            });

            int participantCount = expense.getParticipantMemberIds().size();
            if (participantCount > 0) {
                BigDecimal splitAmount = expense.getTotalAmount()
                        .divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);

                expense.getParticipantMemberIds().forEach(pId -> {
                    owedMap.put(pId, owedMap.getOrDefault(pId, BigDecimal.ZERO).add(splitAmount));
                });
            }
        }

        return activeMembers.stream().map(m -> {
            BigDecimal totalPaid = paidMap.get(m.getId());
            BigDecimal totalOwed = owedMap.get(m.getId());
            BigDecimal balance = totalPaid.subtract(totalOwed);
            
            return new MemberSummary(
                    m.getId(), m.getName(), m.getAvatarColor(), 
                    totalPaid, totalOwed, balance
            );
        }).collect(Collectors.toList());
    }




    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyStatistics(LocalDate from, LocalDate to) {
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(from, to);
        Map<LocalDate, Map<ExpenseType, BigDecimal>> dailyMap = new TreeMap<>();
        Map<LocalDate, Map<String, BigDecimal>> dailyPayersMap = new HashMap<>(); // Đổi sang lưu Map<Tên, Số tiền>

        for (Expense e : expenses) {
            dailyMap.putIfAbsent(e.getExpenseDate(), new EnumMap<>(ExpenseType.class));
            Map<ExpenseType, BigDecimal> typeMap = dailyMap.get(e.getExpenseDate());
            typeMap.put(e.getType(), typeMap.getOrDefault(e.getType(), BigDecimal.ZERO).add(e.getTotalAmount()));

            // Cộng dồn số tiền theo từng người trả trong ngày
            dailyPayersMap.putIfAbsent(e.getExpenseDate(), new HashMap<>());
            Map<String, BigDecimal> dayPayers = dailyPayersMap.get(e.getExpenseDate());
            e.getPayers().forEach(payer -> {
                String name = payer.getMember().getName();
                BigDecimal amount = payer.getAmountPaid();
                dayPayers.put(name, dayPayers.getOrDefault(name, BigDecimal.ZERO).add(amount));
            });
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<ExpenseType, BigDecimal>> entry : dailyMap.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", entry.getKey());
            
            BigDecimal breakfast = entry.getValue().getOrDefault(ExpenseType.BREAKFAST, BigDecimal.ZERO);
            BigDecimal lunch = entry.getValue().getOrDefault(ExpenseType.LUNCH, BigDecimal.ZERO);
            BigDecimal dinner = entry.getValue().getOrDefault(ExpenseType.DINNER, BigDecimal.ZERO);
            BigDecimal shared = entry.getValue().getOrDefault(ExpenseType.SHARED, BigDecimal.ZERO);
            
            row.put("breakfast", breakfast);
            row.put("lunch", lunch);
            row.put("dinner", dinner);
            row.put("shared", shared);
            row.put("total", breakfast.add(lunch).add(dinner).add(shared));
            
            // Lấy Map chi tiết người trả và đẩy xuống Frontend
            Map<String, BigDecimal> payersMap = dailyPayersMap.getOrDefault(entry.getKey(), new HashMap<>());
            row.put("payers", String.join(", ", payersMap.keySet())); 
            row.put("payerDetails", payersMap); // Cung cấp chi tiết tiền từng người
            
            result.add(row);
        }
        return result;
    }
}

