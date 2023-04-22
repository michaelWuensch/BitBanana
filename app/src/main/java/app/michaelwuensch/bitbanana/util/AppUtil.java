package app.michaelwuensch.bitbanana.util;

import android.content.Context;

import com.jakewharton.processphoenix.ProcessPhoenix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class AppUtil {

    private static final String LOG_TAG = AppUtil.class.getSimpleName();

    private static AppUtil mInstance = null;
    private static Context mContext = null;


    private AppUtil() {
        ;
    }

    public static AppUtil getInstance(Context ctx) {

        mContext = ctx;

        if (mInstance == null) {
            mInstance = new AppUtil();
        }

        return mInstance;
    }


    /**
     * Use this function to load a JSON file from res/raw folder.
     *
     * @param id resource id
     * @return The JSON file as string.
     */
    public String loadJSONFromResource(int id) {
        String json = null;
        try {

            InputStream inputStream = mContext.getResources().openRawResource(id);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, StandardCharsets.UTF_8);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void restartApp() {
        ProcessPhoenix.triggerRebirth(mContext);
    }
}
