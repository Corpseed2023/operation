package com.doc.dto.account.vendor;

public enum VendorVoucherLedgerSource {

    /*
     * Account Service resolves the vendor ledger using operationVendorId.
     */
    VENDOR_LEDGER,

    /*
     * Operation Service sends an existing Account Service ledger ID.
     */
    EXISTING_LEDGER
}