package com.fieldforcepro.service;

import com.fieldforcepro.model.Invoice;
import com.fieldforcepro.model.InvoiceAudit;
import com.fieldforcepro.model.InvoiceItem;
import com.fieldforcepro.repository.InvoiceAuditRepository;
import com.fieldforcepro.repository.InvoiceItemRepository;
import com.fieldforcepro.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final InvoiceAuditRepository auditRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceItemRepository itemRepository,
                          InvoiceAuditRepository auditRepository) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.auditRepository = auditRepository;
    }

    public record InvoiceItemPayload(Long productId,
                                     String name,
                                     String sku,
                                     BigDecimal unitPrice,
                                     Integer quantity,
                                     BigDecimal discount,
                                     BigDecimal tax,
                                     BigDecimal lineTotal) { }

    public record InvoicePayload(
            String agentId,
            String createdBy,
            String customerId,
            String customerSnapshotJson,

            // Company / billed-by snapshot
            String companyName,
            String companyAddress,
            String companyGst,
            String companyMobile,
            String companyEmail,

            // Agent snapshot (optional, for PDF builder)
            String agentName,
            String agentPhone,
            String agentEmail,
            String agentDepartment,

            // Personal identifiers (optional)
            String panCard,
            String aadhaarCard,

            // Billed-to extras
            String customerAddress,
            String customerGst,
            String customerMobile,
            String customerEmail,

            List<InvoiceItemPayload> items,
            BigDecimal subtotal,
            BigDecimal totalDiscount,
            BigDecimal taxAmount,
            BigDecimal shipping,
            BigDecimal total,
            String currency,
            String status,
            String notes,

            // Bank / UPI
            String bankName,
            String bankAccountNumber,
            String bankHolderName,
            String ifscCode,
            String accountType,
            String upiId,

            // Terms & conditions / payment terms
            String termsAndConditions,
            String paymentTerms,

            // File URLs (e.g. logo/stamp previously uploaded)
            String companyLogoUrl,
            String companyStampUrl,
            String invoicePdfUrl,

            Instant invoiceDate,
            Instant dueDate
    ) { }

    public record InvoiceSummary(
            String id,
            String invoiceNo,
            String agentId,
            String customerName,
            BigDecimal total,
            String status,
            Instant createdAt
    ) { }

    @Transactional
    public Invoice createInvoice(InvoicePayload payload) {
        validatePayload(payload);

        String invoiceNo = generateInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .agentId(payload.agentId())
                .createdBy(payload.createdBy())
                .customerId(payload.customerId())
                .customerSnapshotJson(payload.customerSnapshotJson())

                .companyName(payload.companyName())
                .companyAddress(payload.companyAddress())
                .companyGst(payload.companyGst())
                .companyMobile(payload.companyMobile())
                .companyEmail(payload.companyEmail())

                .agentName(payload.agentName())
                .agentPhone(payload.agentPhone())
                .agentEmail(payload.agentEmail())
                .agentDepartment(payload.agentDepartment())

                .panCard(payload.panCard())
                .aadhaarCard(payload.aadhaarCard())

                .customerAddress(payload.customerAddress())
                .customerGst(payload.customerGst())
                .customerMobile(payload.customerMobile())
                .customerEmail(payload.customerEmail())

                .subtotal(nullSafe(payload.subtotal()))
                .totalDiscount(nullSafe(payload.totalDiscount()))
                .taxAmount(nullSafe(payload.taxAmount()))
                .shipping(nullSafe(payload.shipping()))
                .total(nullSafe(payload.total()))
                .currency(payload.currency() != null ? payload.currency() : "INR")
                .status(payload.status() != null ? payload.status() : "DRAFT")
                .notes(payload.notes())

                .bankName(payload.bankName())
                .bankAccountNumber(payload.bankAccountNumber())
                .bankHolderName(payload.bankHolderName())
                .ifscCode(payload.ifscCode())
                .accountType(payload.accountType())
                .upiId(payload.upiId())

                .termsAndConditions(payload.termsAndConditions())
                .paymentTerms(payload.paymentTerms())

                .companyLogoUrl(payload.companyLogoUrl())
                .companyStampUrl(payload.companyStampUrl())
                .invoicePdfUrl(payload.invoicePdfUrl())

                .invoiceDate(payload.invoiceDate() != null ? payload.invoiceDate() : Instant.now())
                .dueDate(payload.dueDate())
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        if (payload.items() != null) {
            for (InvoiceItemPayload item : payload.items()) {
                InvoiceItem entity = InvoiceItem.builder()
                        .invoice(saved)
                        .productId(item.productId())
                        .name(item.name())
                        .sku(item.sku())
                        .unitPrice(nullSafe(item.unitPrice()))
                        .quantity(item.quantity() != null ? item.quantity() : 0)
                        .discount(nullSafe(item.discount()))
                        .tax(nullSafe(item.tax()))
                        .lineTotal(nullSafe(item.lineTotal()))
                        .build();
                itemRepository.save(entity);
            }
        }

        audit("CREATED", saved, payload.createdBy(), "Invoice created");
        return saved;
    }

    @Transactional
    public Optional<Invoice> updateInvoice(String id, InvoicePayload payload) {
        Optional<Invoice> existingOpt = invoiceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }
        Invoice existing = existingOpt.get();
        if (!"DRAFT".equalsIgnoreCase(existing.getStatus()) &&
                !"NEW".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Only draft or new invoices can be updated");
        }

        validatePayload(payload);

        existing.setAgentId(payload.agentId());
        existing.setCustomerId(payload.customerId());
        existing.setCustomerSnapshotJson(payload.customerSnapshotJson());

        existing.setCompanyName(payload.companyName());
        existing.setCompanyAddress(payload.companyAddress());
        existing.setCompanyGst(payload.companyGst());
        existing.setCompanyMobile(payload.companyMobile());
        existing.setCompanyEmail(payload.companyEmail());

        existing.setAgentName(payload.agentName());
        existing.setAgentPhone(payload.agentPhone());
        existing.setAgentEmail(payload.agentEmail());
        existing.setAgentDepartment(payload.agentDepartment());

        existing.setPanCard(payload.panCard());
        existing.setAadhaarCard(payload.aadhaarCard());

        existing.setCustomerAddress(payload.customerAddress());
        existing.setCustomerGst(payload.customerGst());
        existing.setCustomerMobile(payload.customerMobile());
        existing.setCustomerEmail(payload.customerEmail());
        existing.setSubtotal(nullSafe(payload.subtotal()));
        existing.setTotalDiscount(nullSafe(payload.totalDiscount()));
        existing.setTaxAmount(nullSafe(payload.taxAmount()));
        existing.setShipping(nullSafe(payload.shipping()));
        existing.setTotal(nullSafe(payload.total()));
        if (payload.currency() != null) {
            existing.setCurrency(payload.currency());
        }
        if (payload.status() != null) {
            existing.setStatus(payload.status());
        }
        existing.setNotes(payload.notes());

        existing.setBankName(payload.bankName());
        existing.setBankAccountNumber(payload.bankAccountNumber());
        existing.setBankHolderName(payload.bankHolderName());
        existing.setIfscCode(payload.ifscCode());
        existing.setAccountType(payload.accountType());
        existing.setUpiId(payload.upiId());

        existing.setTermsAndConditions(payload.termsAndConditions());
        existing.setPaymentTerms(payload.paymentTerms());

        if (payload.companyLogoUrl() != null) {
            existing.setCompanyLogoUrl(payload.companyLogoUrl());
        }
        if (payload.companyStampUrl() != null) {
            existing.setCompanyStampUrl(payload.companyStampUrl());
        }
        if (payload.invoicePdfUrl() != null) {
            existing.setInvoicePdfUrl(payload.invoicePdfUrl());
        }
        if (payload.invoiceDate() != null) {
            existing.setInvoiceDate(payload.invoiceDate());
        }
        existing.setDueDate(payload.dueDate());

        Invoice saved = invoiceRepository.save(existing);

        // Replace items
        List<InvoiceItem> oldItems = itemRepository.findByInvoiceId(saved.getId());
        itemRepository.deleteAll(oldItems);
        if (payload.items() != null) {
            for (InvoiceItemPayload item : payload.items()) {
                InvoiceItem entity = InvoiceItem.builder()
                        .invoice(saved)
                        .productId(item.productId())
                        .name(item.name())
                        .sku(item.sku())
                        .unitPrice(nullSafe(item.unitPrice()))
                        .quantity(item.quantity() != null ? item.quantity() : 0)
                        .discount(nullSafe(item.discount()))
                        .tax(nullSafe(item.tax()))
                        .lineTotal(nullSafe(item.lineTotal()))
                        .build();
                itemRepository.save(entity);
            }
        }

        audit("UPDATED", saved, payload.createdBy(), "Invoice updated");
        return Optional.of(saved);
    }

    @Transactional
    public boolean deleteInvoice(String id, String actorId) {
        Optional<Invoice> existingOpt = invoiceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return false;
        }
        Invoice existing = existingOpt.get();
        List<InvoiceItem> items = itemRepository.findByInvoiceId(id);
        itemRepository.deleteAll(items);
        invoiceRepository.deleteById(id);
        audit("DELETED", existing, actorId, "Invoice deleted");
        return true;
    }

    @Transactional
    public Optional<Invoice> markStatus(String id, String newStatus, String actorId) {
        Optional<Invoice> existingOpt = invoiceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }
        Invoice existing = existingOpt.get();
        existing.setStatus(newStatus);
        Invoice saved = invoiceRepository.save(existing);
        audit(newStatus, saved, actorId, "Status changed to " + newStatus);
        return Optional.of(saved);
    }

    public Page<Invoice> listInvoices(String agentId, String status, LocalDate fromDate, LocalDate toDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (agentId != null && !agentId.isBlank()) {
            if (status != null && !status.isBlank()) {
                return invoiceRepository.findByAgentIdAndStatus(agentId, status, pageable);
            }
            return invoiceRepository.findByAgentId(agentId, pageable);
        }
        // TODO: add date range filtering if needed
        return invoiceRepository.findAll(pageable);
    }

    public Optional<Invoice> findById(String id) {
        return invoiceRepository.findById(id);
    }

    public List<InvoiceItem> getItemsForInvoice(String invoiceId) {
        return itemRepository.findByInvoiceId(invoiceId);
    }

    public List<InvoiceAudit> getAuditForInvoice(String invoiceId) {
        return auditRepository.findByInvoiceIdOrderByCreatedAtAsc(invoiceId);
    }

    @Transactional
    public Optional<Invoice> attachPdf(String id, org.springframework.web.multipart.MultipartFile file, String actorId) throws Exception {
        Optional<Invoice> existingOpt = invoiceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }
        Invoice invoice = existingOpt.get();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }

        String uploadsDir = System.getProperty("user.dir") + "/uploads/invoices";
        java.nio.file.Path dirPath = java.nio.file.Paths.get(uploadsDir);
        java.nio.file.Files.createDirectories(dirPath);

        String filename = id + "-" + System.currentTimeMillis() + ".pdf";
        java.nio.file.Path filePath = dirPath.resolve(filename);
        java.nio.file.Files.write(filePath, file.getBytes());

        // For now expose as a relative path; you can put this behind a static resource handler.
        String url = "/uploads/invoices/" + filename;
        invoice.setInvoicePdfUrl(url);
        Invoice saved = invoiceRepository.save(invoice);

        audit("PDF_ATTACHED", saved, actorId, "PDF uploaded for invoice");
        return Optional.of(saved);
    }

    public InvoiceSummary toSummary(Invoice invoice) {
        String customerName = null;
        // lightweight extraction if snapshot JSON contains name field; can be parsed on frontend too
        if (invoice.getCustomerSnapshotJson() != null && invoice.getCustomerSnapshotJson().contains("\"name\"")) {
            customerName = ""; // leave parsing to frontend to avoid JSON binding here
        }
        return new InvoiceSummary(
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getAgentId(),
                customerName,
                invoice.getTotal(),
                invoice.getStatus(),
                invoice.getCreatedAt()
        );
    }

    private void validatePayload(InvoicePayload payload) {
        if (payload.agentId() == null || payload.agentId().isBlank()) {
            throw new IllegalArgumentException("agentId is required");
        }
        if (payload.createdBy() == null || payload.createdBy().isBlank()) {
            throw new IllegalArgumentException("createdBy is required");
        }
        if (payload.items() == null || payload.items().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        if (payload.subtotal() == null || payload.total() == null) {
            throw new IllegalArgumentException("subtotal and total are required");
        }
        if (payload.subtotal().compareTo(BigDecimal.ZERO) < 0 ||
                payload.total().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amounts cannot be negative");
        }
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generateInvoiceNumber() {
        // Simple pattern: INV-YYYY-<timestamp-last4>
        int year = LocalDate.now(ZoneOffset.UTC).getYear();
        String suffix = String.valueOf(System.currentTimeMillis());
        if (suffix.length() > 4) {
            suffix = suffix.substring(suffix.length() - 4);
        }
        return "INV-" + year + "-" + String.format("%04d", Integer.parseInt(suffix));
    }

    private void audit(String action, Invoice invoice, String actorId, String details) {
        InvoiceAudit audit = InvoiceAudit.builder()
                .invoiceId(invoice.getId())
                .action(action)
                .actorId(actorId != null ? actorId : invoice.getCreatedBy())
                .details(details)
                .build();
        auditRepository.save(audit);
    }
}
