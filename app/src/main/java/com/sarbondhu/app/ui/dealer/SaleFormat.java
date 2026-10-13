package com.sarbondhu.app.ui.dealer;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Sale;
import com.sarbondhu.app.data.model.SaleItem;
import com.sarbondhu.app.ui.admin.AdminRow;
import com.sarbondhu.app.util.BanglaCalendar;
import com.sarbondhu.app.util.BanglaNumber;

import java.util.Calendar;

/** How sales are shown in lists and as an invoice. */
final class SaleFormat {

    private SaleFormat() {
    }

    /** e.g. ১৩/১০/২০২৬ ৪:০৫ PM */
    static String dateTime(long timestamp) {
        Calendar at = Calendar.getInstance();
        at.setTimeInMillis(timestamp);
        String date = at.get(Calendar.DAY_OF_MONTH) + "/" + (at.get(Calendar.MONTH) + 1) + "/"
                + at.get(Calendar.YEAR);
        return BanglaNumber.toBangla(date) + " "
                + BanglaCalendar.getBanglaTime(at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE));
    }

    static String customer(Context context, Sale sale) {
        return sale.getCustomerName().isEmpty()
                ? context.getString(R.string.walk_in_customer) : sale.getCustomerName();
    }

    static AdminRow row(Context context, Sale sale) {
        return new AdminRow(sale.getId(), sale.getInvoiceNo() + " · " + customer(context, sale),
                dateTime(sale.getTimestamp()),
                context.getString(R.string.money, BanglaNumber.format(sale.getTotal())),
                R.color.primary);
    }

    static void showInvoice(Context context, Sale sale) {
        StringBuilder body = new StringBuilder();
        body.append(context.getString(R.string.invoice_customer, customer(context, sale))).append('\n');
        if (!sale.getCustomerPhone().isEmpty()) {
            body.append(context.getString(R.string.invoice_phone,
                    BanglaNumber.toBangla(sale.getCustomerPhone()))).append('\n');
        }
        body.append(context.getString(R.string.invoice_date, dateTime(sale.getTimestamp()))).append("\n\n");
        for (SaleItem item : sale.getItems()) {
            body.append(context.getString(R.string.invoice_line, item.getFertilizerName(),
                    BanglaNumber.format(item.getQuantity()), item.getUnit(),
                    BanglaNumber.format(item.getUnitPrice()),
                    BanglaNumber.format(item.getLineTotal()))).append('\n');
        }
        body.append('\n').append(context.getString(R.string.invoice_total,
                BanglaNumber.format(sale.getTotal())));
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.invoice_title, sale.getInvoiceNo()))
                .setMessage(body.toString())
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
