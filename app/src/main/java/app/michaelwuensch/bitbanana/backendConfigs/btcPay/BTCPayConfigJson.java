package app.michaelwuensch.bitbanana.backendConfigs.btcPay;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BTCPayConfigJson {

    @SerializedName("configurations")
    List<BTCPayConfig> configurations;

    public BTCPayConfig getConfiguration(@NonNull String type, @NonNull String cryptoCode) {
        if (configurations == null)
            return null;
        for (BTCPayConfig btcPayConfig : configurations) {
            if (btcPayConfig.getType().toLowerCase().equals(type.toLowerCase())
                    && btcPayConfig.getCryptoCode().toLowerCase().equals(cryptoCode.toLowerCase())) {
                return btcPayConfig;
            }
        }

        return null;
    }

}
