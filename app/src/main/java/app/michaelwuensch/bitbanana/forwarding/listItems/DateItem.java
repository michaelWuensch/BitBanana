package app.michaelwuensch.bitbanana.forwarding.listItems;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateItem extends ForwardingListItem {

    public long mDate; // in milliseconds

    public DateItem(long timestamp) {

        // The timestamp for forwarding events is provided in nano seconds. The function to format the date later needs milliseconds.
        mDate = timestamp / 1000000L;

        // To get the date line to show up at the correct position in the sorted list, we have to set its timestamp correctly.
        // We set it to 1 nanosecond before the day ends.
        // 86400000000000 = Nanoseconds of one day (60 * 60 * 24 * 1000 * 1000 * 1000)
        String tempDateText = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(mDate);
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(tempDateText);
            mTimestampNS = ((d.getTime() * 1000000L) + (86400000000000L - 1));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getType() {
        return TYPE_DATE;
    }
}
