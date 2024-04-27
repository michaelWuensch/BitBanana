package app.michaelwuensch.bitbanana.backends.lndHub.models;

import com.google.gson.annotations.SerializedName;

public class LndHubAuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
