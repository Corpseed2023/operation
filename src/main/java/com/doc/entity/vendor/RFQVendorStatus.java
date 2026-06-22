package com.doc.entity.vendor;

public enum RFQVendorStatus {
    ADDED,                  // Vendor added in RFQ but not sent yet
    SENT,                   // RFQ sent to vendor
    VIEWED,                 // Vendor opened/viewed RFQ, optional
    QUOTATION_RECEIVED,     // Quotation entered/received
    NO_RESPONSE,            // Vendor did not respond
    DECLINED,               // Vendor refused
    SHORTLISTED,            // Optional comparison stage
    SELECTED,               // Final selected vendor
    REJECTED                // Not selected
}