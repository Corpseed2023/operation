package com.doc.dto.project;

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
public class GovernmentFeePaymentPostingRequestDto {

    private Long operationExpenseId;
    private Long projectId;
    private String projectNo;
    private ExpensePaidBy paidBy;

    private Long paymentBankLedgerId;
    private String paymentBankName;

    private BigDecimal amount;
    private String currencyCode;
    private LocalDate paymentDate;
    private String paymentMode;
    private String paymentReference;
    private String paymentReceiptUrl;

    private Long paidByUserId;
    private String paidByUserName;
    private String narration;
}
