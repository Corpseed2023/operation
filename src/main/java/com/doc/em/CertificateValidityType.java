package com.doc.em;

/**
 * Defines the validity nature of a certificate/license.
 *
 * FIXED_TERM:
 * Certificate has a defined validity period and expiry date.
 *
 * Example:
 * FSSAI License valid for 5 years.
 *
 * LIFETIME:
 * Certificate does not expire.
 *
 * Example:
 * Certain lifetime registrations/licenses.
 */
public enum CertificateValidityType {

    /**
     * Certificate has a fixed validity period.
     *
     * Expected fields:
     * - certificateIssueDate
     * - certificationTenure
     * - certificationTenureUnit
     * - certificateExpiryDate
     *
     * Renewal may or may not be applicable depending
     * on ProductComplianceRule.
     */
    FIXED_TERM,

    /**
     * Certificate remains valid for lifetime.
     *
     * Expected:
     * - certificateIssueDate
     *
     * Normally null:
     * - certificationTenure
     * - certificationTenureUnit
     * - certificateExpiryDate
     * - renewalDueDate
     *
     * Annual return may still be applicable independently.
     */
    LIFETIME
}