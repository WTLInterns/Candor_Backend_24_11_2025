package com.fieldforcepro.repository;

import com.fieldforcepro.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Page<Invoice> findByAgentId(String agentId, Pageable pageable);

    Page<Invoice> findByAgentIdAndStatus(String agentId, String status, Pageable pageable);
}
