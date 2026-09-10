package expense_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import expense_manager.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    
}
