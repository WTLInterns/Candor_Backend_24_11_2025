package com.fieldforcepro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String invoiceNo;

    @Column(nullable = false, length = 36)
    private String agentId;

    @Column(nullable = false, length = 255)
    private String createdBy;

    @Column(length = 36)
    private String customerId;

    @Lob
    @Column(name = "customer_snapshot")
    private String customerSnapshotJson;

    // Company / billed-by snapshot
    @Column(length = 255)
    private String companyName;

    @Column(length = 500)
    private String companyAddress;

    @Column(length = 50)
    private String companyGst;

    @Column(length = 50)
    private String companyMobile;

    @Column(length = 255)
    private String companyEmail;

    // Agent snapshot (optional, for PDF builder)
    @Column(length = 255)
    private String agentName;

    @Column(length = 50)
    private String agentPhone;

    @Column(length = 255)
    private String agentEmail;

    @Column(length = 255)
    private String agentDepartment;

    // Personal identifiers (optional)
    @Column(length = 50)
    private String panCard;

    @Column(length = 50)
    private String aadhaarCard;

    // Billed-to extras (beyond generic customer snapshot JSON)
    @Column(length = 500)
    private String customerAddress;

    @Column(length = 50)
    private String customerGst;

    @Column(length = 50)
    private String customerMobile;

    @Column(length = 255)
    private String customerEmail;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDiscount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shipping;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 8)
    private String currency; // e.g. "INR"

    @Column(nullable = false, length = 32)
    private String status; // DRAFT, SENT, PAID, CANCELLED

    @Column(length = 1000)
    private String notes;

    // Bank / UPI details snapshot
    @Column(length = 255)
    private String bankName;

    @Column(length = 255)
    private String bankAccountNumber;

    @Column(length = 255)
    private String bankHolderName;

    @Column(length = 50)
    private String ifscCode;

    @Column(length = 50)
    private String accountType;

    @Column(length = 255)
    private String upiId;

    // Terms & conditions / payment terms snapshot
    @Column(length = 2000)
    private String termsAndConditions;

    @Column(length = 500)
    private String paymentTerms;

    // File URLs for branding and generated PDF
    @Column(length = 1000)
    private String companyLogoUrl;

    @Column(length = 1000)
    private String companyStampUrl;

    @Column(length = 1000)
    private String invoicePdfUrl;

    @Column(nullable = false)
    private Instant invoiceDate;

    @Column
    private Instant dueDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (currency == null) {
            currency = "INR";
        }
        if (status == null) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
