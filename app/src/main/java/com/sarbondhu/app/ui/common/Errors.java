package com.sarbondhu.app.ui.common;

import androidx.annotation.StringRes;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.repository.DataException;

/** Bangla message for each business-rule failure. */
public final class Errors {

    private Errors() {
    }

    @StringRes
    public static int messageFor(DataException.Reason reason) {
        switch (reason) {
            case USERNAME_TAKEN:
                return R.string.error_username_taken;
            case NEGATIVE_STOCK:
                return R.string.error_negative_stock;
            case INVALID_AMOUNT:
                return R.string.error_invalid_number;
            case INSUFFICIENT_STOCK:
                return R.string.error_insufficient_stock;
            case EMPTY_CART:
                return R.string.error_empty_cart;
            case DEALER_INACTIVE:
                return R.string.error_dealer_inactive;
            case WRONG_PASSWORD:
                return R.string.error_wrong_current_password;
            default:
                return R.string.error_not_found_item;
        }
    }
}
