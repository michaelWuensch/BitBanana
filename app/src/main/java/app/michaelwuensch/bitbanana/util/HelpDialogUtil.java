package app.michaelwuensch.bitbanana.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import app.michaelwuensch.bitbanana.R;

public class HelpDialogUtil {

    public static void showDialog(Context context, int StringResource) {
        LayoutInflater adbInflater = LayoutInflater.from(context);
        View titleView = adbInflater.inflate(R.layout.help_dialog_title, null);

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setCustomTitle(titleView)
                .setMessage(StringResource)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();

        // Make <a href=".."></a> links clickable in message
        TextView messageTextView = alertDialog.findViewById(android.R.id.message);
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}


