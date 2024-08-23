package app.michaelwuensch.bitbanana.contacts;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.UUID;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.models.DecodedBolt12;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;

public class Contact implements Comparable<Contact>, Serializable {

    private String id;
    private String contactData;
    private String alias;
    private ContactType contactType;

    public Contact(String id, ContactType contactType, String contactData, String alias) {
        this.id = id;
        this.contactType = contactType;
        this.contactData = contactData;
        this.alias = alias;
    }

    public String getId() {
        return this.id;
    }

    public String getContactData() {
        return this.contactData;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ContactType getContactType() {
        return this.contactType;
    }

    public LightningNodeUri getAsNodeUri() {
        return LightningNodeUriParser.parseNodeUri(contactData);
    }

    public LnAddress getLightningAddress() {
        return new LnAddress(this.contactData);
    }

    public DecodedBolt12 getBolt12Offer() {
        try {
            return InvoiceUtil.decodeBolt12(this.contactData);
        } catch (Exception e) {
            return null;
        }
    }

    // Used for item adapter
    public String getContent() {
        return this.alias + this.contactData.toLowerCase();
    }


    @Override
    public int compareTo(Contact contact) {
        Contact other = contact;
        return this.getAlias().toLowerCase().compareTo(other.getAlias().toLowerCase());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Contact contact = (Contact) obj;
        return contact.getContactData().equalsIgnoreCase(this.getContactData());
    }

    @Override
    public int hashCode() {
        if (this.id == null) {
            // Create the UUID for the new config
            this.id = UUID.randomUUID().toString();
        }
        return this.id.hashCode();
    }

    public enum ContactType {
        NODEPUBKEY,
        LNADDRESS,
        BOLT12_OFFER;

        public static Contact.ContactType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return NODEPUBKEY;
            }
        }

        public int getTitle() {
            switch (this) {
                case NODEPUBKEY:
                    return R.string.node;
                case LNADDRESS:
                    return R.string.ln_address;
                case BOLT12_OFFER:
                    return R.string.bolt12_offer;
                default:
                    return R.string.node;
            }
        }
    }
}
