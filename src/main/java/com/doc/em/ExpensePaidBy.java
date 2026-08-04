package com.doc.em;

public enum ExpensePaidBy {

    /**
     * Client deposits money into a company bank.
     * Company will later pay the government.
     */
    CLIENT_TO_COMPANY,

    /**
     * Company bears the government fee.
     */
    COMPANY,

    /**
     * Client pays the government portal directly.
     */
    CLIENT_DIRECT,

    /**
     * Legacy value. Treat like CLIENT_DIRECT.
     */
    @Deprecated
    CLIENT
}