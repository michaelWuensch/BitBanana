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
        return AvathorFactory.getAvathor(context, input, PrefsUtil.getAvatarSet());
    }

    public static Bitmap getAvathorWithCache(Context context, String input, int cacheWidth) {
        Bitmap avatar;
        String cacheKey = input + cacheWidth + PrefsUtil.getAvatarSet().name();
        avatar = ImageCache.getInstance(context).getImageFromMemoryCache(cacheKey);
        if (avatar != null) {
            BBLog.v(LOG_TAG, "Avatar loaded from memory cache");
        } else {
            BBLog.v(LOG_TAG, "Avatar image memory cache created");
            avatar = AvathorFactory.getAvathor(context, input, PrefsUtil.getAvatarSet());
            if (cacheWidth > 0) {
                avatar = Bitmap.createScaledBitmap(avatar, cacheWidth, cacheWidth, true);
            }
            //int bitmapByteCount = avatar.getRowBytes() * avatar.getHeight();
            //BBLog.w(LOG_TAG, "size= " + bitmapByteCount);
            ImageCache.getInstance(context).addImageToMemoryCache(cacheKey, avatar);
        }
        return avatar;
    }
}