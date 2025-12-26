
package com.khstay.myapplication.common;

public final class IntentKeys {
    private IntentKeys() {}

    // Canonical key to pass the rental document ID to the detail screen
    public static final String RENTAL_ID = "rentalId";

    // Backward-compatibility (if some old code still uses "propertyId")
    public static final String PROPERTY_ID = "propertyId";
}
