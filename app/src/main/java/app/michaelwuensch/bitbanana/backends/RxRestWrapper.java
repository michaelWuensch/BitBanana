package app.michaelwuensch.bitbanana.backends;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.function.Function;

import app.michaelwuensch.bitbanana.util.BBLog;
import io.reactivex.rxjava3.core.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxRestWrapper {
    private static boolean debug = false;

    private static final String LOG_TAG = RxRestWrapper.class.getSimpleName();

    public static <T, R> Single<R> makeRxCall(OkHttpClient client, Request request, Class<T> jsonResponseClass, Function<T, R> mapper) {
        return DefaultSingle.create(emitter -> {
            if (debug)
                BBLog.d(LOG_TAG, "Execute request " + request.url());
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // Code to be executed on the main thread
                            if (!emitter.isDisposed()) {
                                emitter.onError(e);
                                BBLog.w(LOG_TAG, request.url() + " failed: " + e.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (!emitter.isDisposed()) {
                                if (response.isSuccessful() && response.body() != null) {
                                    try {
                                        String responseAsString = response.body().string();
                                        if (debug)
                                            BBLog.d(LOG_TAG, "Response for " + request.url() + ": " + responseAsString);
                                        if (jsonResponseClass == null)
                                            emitter.onSuccess(mapper.apply(null));
                                        else {
                                            T result = new Gson().fromJson(responseAsString, jsonResponseClass);
                                            R mappedData = mapper.apply(result);
                                            emitter.onSuccess(mappedData);
                                        }
                                    } catch (Exception e) {
                                        BBLog.w(LOG_TAG, e.getMessage());
                                        emitter.onError(e);
                                    } finally {
                                        response.body().close();
                                    }
                                } else {
                                    try {
                                        String responseAsString = response.body().string();
                                        RestErrorResponse errorResponse = new Gson().fromJson(responseAsString, RestErrorResponse.class);
                                        if (errorResponse.getError() && errorResponse.getMessage() != null) {
                                            BBLog.w(LOG_TAG, "Response failed: " + errorResponse.getMessage());
                                            emitter.onError(new RuntimeException(errorResponse.getMessage()));
                                        } else if (errorResponse.getDetail() != null) {
                                            BBLog.w(LOG_TAG, "Response failed: " + errorResponse.getDetail());
                                            emitter.onError(new RuntimeException(errorResponse.getMessage()));
                                        } else {
                                            BBLog.w(LOG_TAG, "Response failed: " + response.code());
                                            BBLog.w(LOG_TAG, responseAsString);
                                            emitter.onError(new RuntimeException("Response failed: " + response.code()));
                                        }
                                    } catch (Exception e) {
                                        BBLog.w(LOG_TAG, "Response failed: " + response.code());
                                        emitter.onError(new RuntimeException("Response failed: " + response.code()));
                                    }
                                }
                            }
                        }
                    });
                }
            });
        });
    }
}
