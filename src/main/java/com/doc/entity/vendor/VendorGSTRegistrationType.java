package com.doc.entity.vendor;

public enum VendorGSTRegistrationType {

    INTERNATIONAL,
    SEZ,
    REGISTERED,
    UNREGISTERED;

    public boolean isGstApplicable() {
        return this == REGISTERED
                || this == UNREGISTERED;
    }

    public boolean isZeroRated() {
        return this == SEZ
                || this == INTERNATIONAL;
    }
}