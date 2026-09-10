package expense_manager.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import expense_manager.dto.ExpenseRequest;
import expense_manager.dto.MemberSummary;
import expense_manager.entity.Member;
import expense_manager.repository.MemberRepository;
import expense_manager.service.ExpenseService;
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

    @GetMapping("/members")
    public List<Member> getActiveMembers() {
        return memberRepository.findAll().stream()
                .filter(Member::isActive)
                .toList();
    }

    @PostMapping("/expenses")
    public ResponseEntity<String> createExpense(@Valid @RequestBody ExpenseRequest request) {
        try {
            expenseService.createExpense(request);
            return ResponseEntity.ok("Thêm khoản chi thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/summary")
    public List<MemberSummary> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return summaryService.getSummary(from, to);
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

    @GetMapping("/statistics/daily")
    public List<Map<String, Object>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return summaryService.getDailyStatistics(from, to);
    }
}
