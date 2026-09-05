package com.bundleessential.util;

import java.text.NumberFormat;
import java.util.Locale;

/** Central money display: 1000 -> 1,000.00, 100000 -> 100,000.00, 1000000 -> 1,000,000.00 */
public final class Money {

    private Money() {}

    private static final NumberFormat FORMAT;

    static {
        FORMAT = NumberFormat.getNumberInstance(Locale.US);
        FORMAT.setGroupingUsed(true);
        FORMAT.setMinimumFractionDigits(2);
        FORMAT.setMaximumFractionDigits(2);
    }

    public static synchronized String format(double amount) {
        return FORMAT.format(amount);
    }
}
