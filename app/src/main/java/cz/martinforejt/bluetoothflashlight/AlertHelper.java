package cz.martinforejt.bluetoothflashlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by Martin Forejt on 22.08.2016.
 * forejt.martin97@gmail.com
 */
public class AlertHelper {

    protected Context context;
    protected AlertDialog.Builder dialogBuilder;

    public static AlertHelper Create(Context context) {
        AlertHelper helper = new AlertHelper();
        helper.context = context;
        helper.dialogBuilder = new AlertDialog.Builder(helper.context);
        helper.dialogBuilder.setCancelable(false);

        return helper;
    }

    public static AlertHelper Create(Context context, String title, String message) {
        AlertHelper helper = AlertHelper.Create(context);
        helper.setTitle(title);
        helper.setMessage(message);

        return helper;
    }

    public static AlertHelper Create(Context context, String title, String message, String btn, boolean closeApp) {
        final AlertHelper helper = AlertHelper.Create(context, title, message);
        if(closeApp) {
            helper.addButton(btn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ((Activity) helper.context).finish();
                }
            });
        } else {
            helper.addButton(btn, null);
        }

        return helper;
    }

    public boolean show() {
        AlertDialog dialog = this.dialogBuilder.create();
        dialog.show();
        return true;
    }

    public AlertHelper setTitle(String title) {
        dialogBuilder.setTitle(title);
        return this;
    }

    public AlertHelper setMessage(String message) {
        dialogBuilder.setMessage(message);
        return this;
    }

    public AlertHelper addButton(String text, DialogInterface.OnClickListener listener) {
        dialogBuilder.setNeutralButton(text, listener);
        return this;
    }

    public AlertHelper addButton(String text, boolean positive, DialogInterface.OnClickListener listener) {
        if(positive) {
            dialogBuilder.setPositiveButton(text, listener);
        } else {
            dialogBuilder.setNegativeButton(text, listener);
        }
        return this;
    }

}
