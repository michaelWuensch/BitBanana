package app.michaelwuensch.bitbanana.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.michaelwuensch.bitbanana.util.UriUtil;

public class LnAddress implements Serializable {
    private String mUsername;
    private String mDomain;
    private String mAddress;

    public LnAddress(String address) {
        mAddress = address;
        if (UriUtil.isLightningUri(address) || UriUtil.isLNURLPUri(address))
            mAddress = UriUtil.removeURI(mAddress);
        if (isValidLnurlAddress() || isValidBip353DnsRecordAddress()) {
            String[] parts = mAddress.split("@");
            mUsername = parts[0];
            mDomain = parts[1];
        }
    }

    public String getUsername() {
        return mUsername.replace("₿", "");
    }

    public String getDomain() {
        return mDomain;
    }

    public boolean isTor() {
        return mDomain.endsWith(".onion");
    }

    public boolean isValidLnurlAddress() {
        return validateLnurlFormat(mAddress);
    }

    public boolean isValidBip353DnsRecordAddress() {
        return validateBip353DnsFormat(mAddress);
    }

    @NonNull
    public String toString() {
        return mUsername + "@" + mDomain;
    }

    private boolean validateLnurlFormat(String address) {
        /* Simplified regex checking the following:
            - the string has no white spaces
            - username is a lowercase alphanumeric (including "." and "_") string of at least one character
            - username is followed by exactly one "@"
            - domain name has at leas one "."
            - domain name uses only alphanumeric character (including "-")
        */
        String regexPattern = "^[a-z0-9_.]+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]+$";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(address);

        return matcher.matches();
    }

    private static boolean validateBip353DnsFormat(String address) {
        /* Simplified regex checking the following:
            - the string may start with "₿"
            - the string has no white spaces
            - username is an alphanumeric (including ".", "_" and "-") string of at least one character
            - username is followed by exactly one "@"
            - domain name has at leas one "."
            - domain name uses only alphanumeric character (including "-")
        */
        String regexPattern = "^₿?[a-zA-Z0-9_.-]+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]+$";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(address);

        return matcher.matches();
    }
}
