package app.michaelwuensch.bitbanana.listViews.contacts;

import app.michaelwuensch.bitbanana.contacts.Contact;

public interface ContactSelectListener {

    void onContactSelect(Contact contact, boolean clickOnAvatar);
}
