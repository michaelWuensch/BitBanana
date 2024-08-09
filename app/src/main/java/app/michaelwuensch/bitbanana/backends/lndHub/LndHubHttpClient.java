package app.michaelwuensch.bitbanana.backends.lndHub;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.BackendManager;
import app.michaelwuensch.bitbanana.backends.lndHub.models.LndHubAuthResponse;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Singleton to handle the LndHub http client
 */
public class LndHubHttpClient {
    private static LndHubHttpClient mHttpClientInstance;
    private OkHttpClient mHttpClient;
    private static final String LOG_TAG = LndHubHttpClient.class.getSimpleName();


    private LndHubHttpClient() {
    }

    public void createHttpClient() {
        if (BackendManager.getCurrentBackendConfig().getUseTor()) {
            Proxy torProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", TorManager.getInstance().getSocksProxyPort()));

            mHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AccessTokenInterceptor())
                    .authenticator(new TokenRefreshAuthenticator())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .proxy(torProxy)
                    .build();
            BBLog.d(LOG_TAG, "LndHubHttpClient created. Socks Proxy Port: " + TorManager.getInstance().getSocksProxyPort());
        } else {
            mHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AccessTokenInterceptor())
                    .authenticator(new TokenRefreshAuthenticator())
                    .build();
            BBLog.d(LOG_TAG, "LndHubHttpClient created.");
        }
    }

    public void restartHttpClient() {
        BBLog.d(LOG_TAG, "Restarting LndHubHttpClient.");
        if (mHttpClient != null)
            mHttpClient.dispatcher().cancelAll();
        createHttpClient();
    }

    public static synchronized LndHubHttpClient getInstance() {
        if (mHttpClientInstance == null) {
            mHttpClientInstance = new LndHubHttpClient();
        }
        return mHttpClientInstance;
    }

    public OkHttpClient getClient() {
        return mHttpClient;
    }

    public void cancelAllRequests() {
        if (mHttpClient != null)
            mHttpClient.dispatcher().cancelAll();
    }

    public static class AccessTokenInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder builder = originalRequest.newBuilder();
            String accessToken = BackendManager.getCurrentBackendConfig().getTempAccessToken();
            if (accessToken != null) {
                builder.header("Authorization", "Bearer " + accessToken);
            }
            Request requestWithTempAccessToken = builder.build();
            Response response = chain.proceed(requestWithTempAccessToken);

            // Normally the TokenRefreshAuthenticators authenticate method gets called automatically through a 401 response.
            // But the original LndHub implementation does return success with the following json instead:
            // {"error":true,"code":1,"message":"bad auth"}
            // Therefore, we also check if the response body contains the "error": true structure
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.peekBody(Long.MAX_VALUE).string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.optBoolean("error") && jsonResponse.optInt("code") == 1 && "bad auth".equals(jsonResponse.optString("message"))) {
                        BBLog.i(LOG_TAG, "Detected bad auth error, refreshing token");

                        // Call the TokenRefreshAuthenticator to get a new token
                        TokenRefreshAuthenticator authenticator = new TokenRefreshAuthenticator();
                        Request newRequest = authenticator.authenticate(null, response);

                        if (newRequest != null) {
                            response.close(); // Close the original response
                            return chain.proceed(newRequest); // Retry with the new token
                        }
                    }
                } catch (Exception ignore) {

                }
            }

            return response; // Return the original response if no authentication is needed
        }
    }

    public static class TokenRefreshAuthenticator implements Authenticator {

        public TokenRefreshAuthenticator() {

        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            LndHubAuthResponse newTokens = refreshTokens();

            if (newTokens != null) {
                // Save access & refresh token
                BackendManager.getCurrentBackendConfig().setTempAccessToken(newTokens.getAccessToken());
                BackendManager.getCurrentBackendConfig().setTempRefreshToken(newTokens.getRefreshToken());
                BackendConfigsManager.getInstance().updateBackendConfig(BackendManager.getCurrentBackendConfig());
                try {
                    BackendConfigsManager.getInstance().apply();
                } catch (Exception e) {
                    BBLog.e(LOG_TAG, "Failed to save access and refresh token in backend config");
                }

                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newTokens.getAccessToken())
                        .build();
            }

            return null;
        }

        private LndHubAuthResponse refreshTokens() {
            LndHubAuthResponse newTokens = null;
            newTokens = authWithRefreshToken();
            if (newTokens != null)
                return newTokens;
            else
                return authWithUserCredentials();
        }

        private LndHubAuthResponse authWithRefreshToken() {
            if (BackendManager.getCurrentBackendConfig().getTempRefreshToken() == null)
                return null;

            BBLog.i(LOG_TAG, "New authentication with refresh token requested.");

            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            JSONObject json = new JSONObject();
            try {
                json.put("refresh_token", BackendManager.getCurrentBackendConfig().getTempRefreshToken());
            } catch (Exception e) {
                e.printStackTrace();
            }
            RequestBody body = RequestBody.create(json.toString(), JSON);

            // Create the request to the authentication endpoint
            Request request = new Request.Builder()
                    .url(BackendManager.getCurrentBackendConfig().getHost() + "auth?type=refresh_token")
                    .post(body)
                    .build();

            return makeAuthCall(request);
        }

        private LndHubAuthResponse authWithUserCredentials() {
            BBLog.i(LOG_TAG, "New authentication with user credentials requested.");
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            JSONObject json = new JSONObject();
            try {
                json.put("login", BackendManager.getCurrentBackendConfig().getUser());
                json.put("password", BackendManager.getCurrentBackendConfig().getPassword());
            } catch (Exception e) {
                e.printStackTrace();
            }
            RequestBody body = RequestBody.create(json.toString(), JSON);


            // Create the request to the authentication endpoint
            Request request = new Request.Builder()
                    .url(BackendManager.getCurrentBackendConfig().getHost() + "auth?type=auth")
                    .post(body)
                    .build();

            return makeAuthCall(request);
        }

        private LndHubAuthResponse makeAuthCall(Request request) {
            try {
                // Execute the request synchronously
                Response response = new OkHttpClient().newCall(request).execute();

                // Check if the request was successful
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    return new Gson().fromJson(responseBody, LndHubAuthResponse.class);
                } else {
                    BBLog.e(LOG_TAG, "Lnd Hub auth failed: " + response.body().string());
                }
            } catch (Exception e) {
                BBLog.e(LOG_TAG, "Lnd Hub auth failed.");
                e.printStackTrace();
            }
            return null;
        }
    }
}
