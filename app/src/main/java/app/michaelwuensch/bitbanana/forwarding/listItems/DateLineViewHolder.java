package app.michaelwuensch.bitbanana.forwarding.listItems;

import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import app.michaelwuensch.bitbanana.R;

public class DateLineViewHolder extends ForwardingItemViewHolder {
    private TextView mTvDate;
    private TimeZone timeZone;

    {
        timeZone = TimeZone.getDefault();
    }

    public DateLineViewHolder(View v) {
        super(v);
        mTvDate = v.findViewById(R.id.date);
        mContext = v.getContext();
    }

    public void bindDateItem(DateItem dateItem) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, mContext.getResources().getConfiguration().locale);
        String formattedDate = df.format(new Date(dateItem.mDate - timeZone.getOffset(System.currentTimeMillis())));

        // Check if this date was today or yesterday
        String formattedDateYesterday = df.format(getYesterday());
        String formattedDateToday = df.format(getToday());

        if (formattedDate.equals(formattedDateToday)) {
            mTvDate.setText(mContext.getResources().getString(R.string.today));
        } else {
            if (formattedDate.equals(formattedDateYesterday)) {
                mTvDate.setText(mContext.getResources().getString(R.string.yesterday));
            } else {
                mTvDate.setText(formattedDate);
            }
        }
    }

    private Date getYesterday() {
        return new Date(System.currentTimeMillis() - timeZone.getOffset(System.currentTimeMillis()) - 24 * 60 * 60 * 1000);
    }

    private Date getToday() {
        return new Date(System.currentTimeMillis() - timeZone.getOffset(System.currentTimeMillis()));
    }
}
