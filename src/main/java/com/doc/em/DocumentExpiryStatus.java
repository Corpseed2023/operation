package com.doc.em;

public enum DocumentExpiryStatus {

    /**
     * Expiry date is found and already passed.
     */
    EXPIRED,

    /**
     * Expiry date is coming within next 30 days.
     */
    EXPIRING_SOON,

    /**
     * Expiry date is found and valid for more than 30 days.
     */
    VALID,

    /**
     * Expiry date could not be found from document.
     */
    UNKNOWN
}