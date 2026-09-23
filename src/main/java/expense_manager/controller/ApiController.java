package expense_manager.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import expense_manager.dto.DebtDto;
import expense_manager.dto.ExpenseRequest;
import expense_manager.dto.MemberSummary;
import expense_manager.dto.SettlementRequest;
import expense_manager.entity.Member;
import expense_manager.repository.MemberRepository;
import expense_manager.service.ExpenseService;
import expense_manager.service.SettlementService;
import expense_manager.service.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final MemberRepository memberRepository;
    private final ExpenseService expenseService;
    private final SummaryService summaryService;
    private final SettlementService settlementService;

    // ==================== MEMBERS ====================

    @GetMapping("/members")
    public List<Member> getActiveMembers() {
        return memberRepository.findAll().stream()
                .filter(Member::isActive)
                .toList();
    }

    @PostMapping("/members")
    public ResponseEntity<String> addGuest(@RequestParam String name) {
        Member m = new Member();
        m.setName(name + " (Khách)");
        m.setAvatarColor("#6c757d"); // Màu xám cho khách
        m.setActive(true);
        memberRepository.save(m);
        return ResponseEntity.ok("Thêm khách thành công");
    }

    // ==================== EXPENSES ====================

    @PostMapping("/expenses")
    public ResponseEntity<String> createExpense(@Valid @RequestBody ExpenseRequest request) {
        expenseService.createExpense(request);
        return ResponseEntity.ok("Thêm khoản chi thành công");
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<String> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        expenseService.updateExpense(id, request);
        return ResponseEntity.ok("Cập nhật hóa đơn thành công");
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok("Xóa hóa đơn thành công");
    }

    // ==================== SUMMARY & STATISTICS ====================

    @GetMapping("/summary")
    public List<MemberSummary> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return summaryService.getSummary(from, to);
    }

    @GetMapping("/statistics/daily")
    public List<Map<String, Object>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return summaryService.getDailyStatistics(from, to);
    }

    @GetMapping("/statistics/history")
    public ResponseEntity<List<Map<String, Object>>> getDetailedExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(summaryService.getDetailedExpenses(from, to));
    }

    // ==================== SETTLEMENTS (THANH TOÁN NỢ) ====================

    @GetMapping("/debts")
    public List<DebtDto> getDebts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return settlementService.calculateDebts(from, to);
    }

    @PostMapping("/settlements")
    public ResponseEntity<String> createSettlement(@Valid @RequestBody SettlementRequest request) {
        settlementService.createSettlement(request);
        return ResponseEntity.ok("Ghi nhận thanh toán thành công");
    }

    @GetMapping("/settlements")
    public List<Map<String, Object>> getSettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return settlementService.getSettlementHistory(from, to);
    }

    @DeleteMapping("/settlements/{id}")
    public ResponseEntity<String> deleteSettlement(@PathVariable Long id) {
        settlementService.deleteSettlement(id);
        return ResponseEntity.ok("Đã hủy thanh toán");
    }
}
