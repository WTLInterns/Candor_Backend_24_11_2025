package com.fieldforcepro.controller;

import com.fieldforcepro.model.Invoice;
import com.fieldforcepro.model.InvoiceAudit;
import com.fieldforcepro.model.InvoiceItem;
import com.fieldforcepro.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @Operation(summary = "Create a new invoice")
    public ResponseEntity<Invoice> create(@RequestBody InvoiceService.InvoicePayload payload) {
        Invoice created = invoiceService.createInvoice(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List invoices with optional filters")
    public Page<Invoice> list(
            @RequestParam(value = "agentId", required = false) String agentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;
        return invoiceService.listInvoices(agentId, status, fromDate, toDate, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice detail including line items")
    public ResponseEntity<Map<String, Object>> get(@PathVariable("id") String id) {
        Optional<Invoice> invoiceOpt = invoiceService.findById(id);
        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Invoice invoice = invoiceOpt.get();
        List<InvoiceItem> items = invoiceService.getItemsForInvoice(id);
        List<InvoiceAudit> audits = invoiceService.getAuditForInvoice(id);
        return ResponseEntity.ok(Map.of(
                "invoice", invoice,
                "items", items,
                "audit", audits
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing invoice (only if draft/new)")
    public ResponseEntity<Invoice> update(@PathVariable("id") String id,
                                          @RequestBody InvoiceService.InvoicePayload payload) {
        Optional<Invoice> updated = invoiceService.updateInvoice(id, payload);
        return updated
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an invoice")
    public ResponseEntity<Void> delete(@PathVariable("id") String id,
                                       @RequestParam("actorId") String actorId) {
        boolean deleted = invoiceService.deleteInvoice(id, actorId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Mark invoice as SENT")
    public ResponseEntity<Invoice> send(@PathVariable("id") String id,
                                        @RequestParam("actorId") String actorId) {
        Optional<Invoice> updated = invoiceService.markStatus(id, "SENT", actorId);
        return updated
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Mark invoice as PAID")
    public ResponseEntity<Invoice> pay(@PathVariable("id") String id,
                                       @RequestParam("actorId") String actorId) {
        Optional<Invoice> updated = invoiceService.markStatus(id, "PAID", actorId);
        return updated
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/pdf")
    @Operation(summary = "Upload generated invoice PDF and attach to invoice")
    public ResponseEntity<Invoice> uploadPdf(@PathVariable("id") String id,
                                             @RequestPart("file") MultipartFile file,
                                             @RequestParam(value = "actorId", required = false) String actorId) {
        try {
            Optional<Invoice> updated = invoiceService.attachPdf(id, file, actorId);
            return updated
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
