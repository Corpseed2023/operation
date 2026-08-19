package com.doc.dto;

import com.doc.em.ExpensePaidBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentFeePostingRequestDto {
    private Long operationExpenseId;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private Long clientCompanyId;
    private String clientCompanyName;
    private Long clientUnitId;
    private String clientUnitName;

    /**
     * Optional explicit override of the client's own CUSTOMER ledger in
     * Account Service (e.g. Microsoft). When null, Account Service
     * auto-resolves the ledger from clientCompanyId + clientUnitId.
     */
    private Long clientLedgerId;

    private String expenseCategory;
    private BigDecimal approvedAmount;
    private String currencyCode;
    private LocalDate expenseDate;
    private ExpensePaidBy paidBy;
    private String clientPaymentMode;
    private Long clientPaymentBankLedgerId;
    private String clientPaymentBankName;
    private LocalDate clientPaymentDate;
    private String clientPaymentReference;
    private String clientPaymentProofUrl;
    private Long approvedByUserId;
    private String approvedByUserName;
    private String narration;
}