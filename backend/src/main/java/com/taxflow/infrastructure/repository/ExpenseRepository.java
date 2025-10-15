package com.taxflow.infrastructure.repository;

import com.taxflow.domain.enums.ExpenseCategory;
import com.taxflow.domain.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {
    Page<Expense> findByBusinessId(UUID businessId, Pageable pageable);
    List<Expense> findTop10ByBusinessIdOrderByExpenseDateDesc(UUID businessId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.business.id = :businessId and e.expenseDate between :from and :to")
    BigDecimal sumAmount(UUID businessId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(e.gstAmount), 0) from Expense e where e.business.id = :businessId and e.expenseDate between :from and :to")
    BigDecimal sumGst(UUID businessId, LocalDate from, LocalDate to);

    @Query("select e.category, coalesce(sum(e.amount), 0) from Expense e where e.business.id = :businessId and e.expenseDate between :from and :to group by e.category")
    List<Object[]> categoryBreakdown(UUID businessId, LocalDate from, LocalDate to);

    long countByBusinessIdAndCategory(UUID businessId, ExpenseCategory category);
}
