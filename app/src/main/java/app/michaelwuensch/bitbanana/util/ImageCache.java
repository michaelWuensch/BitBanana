package app.michaelwuensch.bitbanana.util;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.collection.LruCache;


public class ImageCache {

    private static final int MEMORY_CACHE_SIZE = 1024 * 1024 * 5; // 5MB

    private LruCache<String, Bitmap> mLruCache;

    private static ImageCache mImageCache;

    public static ImageCache getInstance(Context context) {
        if (mImageCache == null) {
            mImageCache = new ImageCache();
            mImageCache.initializeCache(context);
        }

        return mImageCache;
    }

    public void initializeCache(Context context) {

        // Init memory cache
        mLruCache = new LruCache<String, Bitmap>(MEMORY_CACHE_SIZE) {
            protected int sizeOf(String key, Bitmap value) {
                // The cache size will be measured in bytes rather than number of items.

                int bitmapByteCount = value.getRowBytes() * value.getHeight();

                return bitmapByteCount;
            }
        };

        // Initialize disk cache
        // ToDo: Disk cache
    }


    public void addImageToMemoryCache(String key, Bitmap value) {
        if (mLruCache != null && mLruCache.get(key) == null) {
            mLruCache.put(key, value);
        }
    }

    public Bitmap getImageFromMemoryCache(String key) {
        if (mLruCache != null && key != null) {
            return mLruCache.get(key);
        } else {
            return null;
        }
    }

    public void removeImageFromMemoryCache(String key) {
        mLruCache.remove(key);
    }

    public void clearMemoryCache() {
        if (mLruCache != null) {
            mLruCache.evictAll();
        }
    }
}


