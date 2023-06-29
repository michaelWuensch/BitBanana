package app.michaelwuensch.bitbanana.contacts;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.michaelwuensch.avathorlibrary.AvathorFactory;

import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.R;

public class ContactItemViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = ContactItemViewHolder.class.getSimpleName();

    private ImageFilterView mUserAvatar;
    private TextView mContactName;
    private View mRootView;
    private Context mContext;
    private ContactSelectListener mContactSelectListener;

    public ContactItemViewHolder(View v) {
        super(v);

        mUserAvatar = v.findViewById(R.id.userAvatar);
        mContactName = v.findViewById(R.id.contactName);
        mRootView = v.findViewById(R.id.transactionRootView);
        mContext = v.getContext();
    }

    public void bindContactItem(Contact contact) {

        // Set User Avatar
        mUserAvatar.setImageBitmap(AvathorFactory.getAvathor(mContext, contact.getContactData()));

        // Set Contact Name
        mContactName.setText(contact.getAlias());

        // Set on click listener
        mRootView.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mContactSelectListener != null) {
                    mContactSelectListener.onContactSelect(contact, false);
                }
            }
        });

        // Set on click listener for User Avatar
        mUserAvatar.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (mContactSelectListener != null) {
                    mContactSelectListener.onContactSelect(contact, true);
                }
            }
        });
    }

    void addOnContactSelectListener(ContactSelectListener contactSelectListener) {
        mContactSelectListener = contactSelectListener;
    }

    public String getName() {return mContactName.getText().toString();}
}
