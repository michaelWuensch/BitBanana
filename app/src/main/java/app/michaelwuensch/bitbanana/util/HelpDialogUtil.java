package app.michaelwuensch.bitbanana.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

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

        /* This would be nice from a user experience perspective, but it actually allows translators to inject malicious links. :(
        // Make <a href=".."></a> links clickable in message
        TextView messageTextView = alertDialog.findViewById(android.R.id.message);
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        */
    }

    public static void showDialogWithLink(Context context, int StringResource, String linkButtonText, String url) {
        LayoutInflater adbInflater = LayoutInflater.from(context);
        View titleView = adbInflater.inflate(R.layout.help_dialog_title, null);

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setCustomTitle(titleView)
                .setMessage(StringResource)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .setNeutralButton(linkButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(browserIntent);
                    }
                }).show();
    }
}


