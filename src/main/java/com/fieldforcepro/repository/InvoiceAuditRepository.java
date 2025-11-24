package com.fieldforcepro.repository;

import com.fieldforcepro.model.InvoiceAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceAuditRepository extends JpaRepository<InvoiceAudit, Long> {

    List<InvoiceAudit> findByInvoiceIdOrderByCreatedAtAsc(String invoiceId);
}
