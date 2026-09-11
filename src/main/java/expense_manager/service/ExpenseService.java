package expense_manager.service;


import expense_manager.dto.ExpenseRequest;
import expense_manager.entity.Expense;
import expense_manager.entity.ExpensePayer;
import expense_manager.repository.ExpenseRepository;
import expense_manager.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createExpense(ExpenseRequest request) {
        BigDecimal totalPaid = request.payers().stream()
                .map(ExpenseRequest.PayerDto::amountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(request.totalAmount()) != 0) {
            throw new IllegalArgumentException("Tổng số tiền người chi trả không khớp với tổng hóa đơn");
        }

        Expense expense = new Expense();
        expense.setExpenseDate(request.expenseDate());
        expense.setType(request.type());
        expense.setDescription(request.description());
        expense.setTotalAmount(request.totalAmount());
        expense.setParticipantMemberIds(request.participantIds());

        request.payers().forEach(pDto -> {
            ExpensePayer payer = new ExpensePayer();
            payer.setMember(memberRepository.getReferenceById(pDto.memberId()));
            payer.setAmountPaid(pDto.amountPaid());
            expense.addPayer(payer);
        });

        expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn với ID: " + id);
        }
        expenseRepository.deleteById(id);
    }
}
