package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.graphics.Bitmap;

import com.github.michaelwuensch.avathorlibrary.AvathorFactory;

/**
 * Util cClass to get avatar images for contacts.
 */
public class AvathorUtil {
    private static final String LOG_TAG = AvathorUtil.class.getSimpleName();

    public static Bitmap getAvathor(Context context, String input) {
        return AvathorFactory.getAvathor(context, input);
    }

    public static Bitmap getAvathorWithCache(Context context, String input, int cacheWidth) {
        Bitmap avatar;
        avatar = ImageCache.getInstance(context).getImageFromMemoryCache(input + cacheWidth);
        if (avatar != null) {
            BBLog.v(LOG_TAG, "Avatar loaded from memory cache");
        } else {
            BBLog.w(LOG_TAG, "Avatar image memory cache created");
            avatar = AvathorFactory.getAvathor(context, input);
            if (cacheWidth > 0) {
                avatar = Bitmap.createScaledBitmap(avatar, cacheWidth, cacheWidth, true);
            }
            //int bitmapByteCount = avatar.getRowBytes() * avatar.getHeight();
            //BBLog.w(LOG_TAG, "size= " + bitmapByteCount);
            ImageCache.getInstance(context).addImageToMemoryCache(input + cacheWidth, avatar);
        }
        return avatar;
    }
}
