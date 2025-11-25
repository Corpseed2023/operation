package com.doc.em;

public enum DocumentExpiryType {
    FIXED,      // Never expires (Aadhaar, PAN)
    EXPIRING,   // Has expiry date (CTO, Driving License)
    UNKNOWN     // CRT decides during upload
}