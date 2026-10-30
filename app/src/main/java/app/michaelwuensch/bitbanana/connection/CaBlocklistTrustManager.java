package app.michaelwuensch.bitbanana.connection;

import android.net.http.X509TrustManagerExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.net.Socket;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.HexUtil;

/**
 * Wraps the trust manager of the system and additionally rejects every certificate chain that contains the key of a compromised certificate authority.
 * <p>
 * Android does not check revocation and the system trust store of older devices never gets updated.
 * Certificates issued with the key of a compromised CA would therefore still be trusted by many devices, even after browsers distrusted the CA.
 * Like browsers we block by public key (SHA-256 of the SubjectPublicKeyInfo). This also covers cross-signed versions of the same CA certificate.
 * <p>
 * Use this for all TLS connections that rely on the system trust store.
 */
public class CaBlocklistTrustManager extends X509ExtendedTrustManager {

    private static final String LOG_TAG = CaBlocklistTrustManager.class.getSimpleName();

    /**
     * SHA-256 hashes of the SubjectPublicKeyInfo of blocked CA certificates.
     */
    @VisibleForTesting
    static final Set<String> BLOCKED_SPKI_SHA256 = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // eMudhra intermediates with trivially recoverable private keys (October 2026, https://bugzilla.mozilla.org/show_bug.cgi?id=2083449)
            // All of them also exist as cross-signs from Sectigo "AAA Certificate Services", which is still in the Android trust store.
            "ebed6475c2c2d80fcb199f3fe8dd6e6b3ce60b77eb15d4e42c3dec0c6f5b5105", // EM DV TLS CA - G3B
            "49dd353dc2b919a3770645117d0e60c16115e7d1bc259eefe734b00ea434fe5f", // EM DV TLS CA - G3C
            "43bffa19d6ef651451f6841e019931e96f6f2eaeff28ab4b6f1ce4576de2717b", // EM OV TLS CA - G3B
            "63ecefee431d49523bc1136c4e72d0206a477086caf606cd5cf025686c109cf8", // EM OV TLS CA - G3C
            "e0254c7fcdc265e2f0f8a428dabb75ab4ae039b4872d62f5ee57f0877d5a8a41", // EM EV TLS CA - G3B
            "5c0660a2b164910d8ff589c18e14552e9e0ec237dcefe2cb736d41a4811f81a0", // EM EV TLS CA - G3C
            "ec4ffa1ad0ac88081314b7aaffef6a91f97cd57291bb5ba686c5415f5eb4f6b6", // EM OV CS CA - G3B
            "92c063d67e8f14db71d288e11266ffdaf69cbf115f7bab14f3bc2f68d25e0e30", // EM OV CS CA - G3C
            // The root that signed them. Removed from the Chrome and Mozilla root stores.
            "8d417db2dd8bf5e3084d1e3f196d583849d81bdd4c00c70b9d39369e96b8c782"  // emSign ECC Root CA - G3
    )));

    private static CaBlocklistTrustManager mSystemInstance;
    private static SSLSocketFactory mSystemSocketFactory;

    private final X509TrustManager mDelegate;
    @Nullable
    private final X509TrustManagerExtensions mDelegateExtensions;

    @VisibleForTesting
    CaBlocklistTrustManager(@NonNull X509TrustManager delegate) {
        mDelegate = delegate;
        mDelegateExtensions = createExtensions(delegate);
    }

    /**
     * Returns the system trust manager extended by the blocklist.
     */
    public static synchronized CaBlocklistTrustManager getSystemInstance() {
        if (mSystemInstance == null) {
            mSystemInstance = new CaBlocklistTrustManager(getSystemTrustManager());
        }
        return mSystemInstance;
    }

    /**
     * Returns a SSLSocketFactory that uses the system trust manager extended by the blocklist.
     */
    public static synchronized SSLSocketFactory getSystemSocketFactory() {
        if (mSystemSocketFactory == null) {
            mSystemSocketFactory = createSystemSSLContext(null).getSocketFactory();
        }
        return mSystemSocketFactory;
    }

    /**
     * Creates a SSLContext that uses the system trust manager extended by the blocklist.
     *
     * @param keyManagers Key managers for client authentication or null.
     */
    public static SSLContext createSystemSSLContext(@Nullable KeyManager[] keyManagers) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, new TrustManager[]{getSystemInstance()}, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            // This should never happen. Without a working TLS implementation no secure connection is possible at all.
            throw new IllegalStateException("Unable to create SSLContext.", e);
        }
    }

    private static X509TrustManager getSystemTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager)
                    return (X509TrustManager) trustManager;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to get the system trust manager.", e);
        }
        throw new IllegalStateException("No X509TrustManager available.");
    }

    @Nullable
    private static X509TrustManagerExtensions createExtensions(X509TrustManager delegate) {
        try {
            return new X509TrustManagerExtensions(delegate);
        } catch (Exception e) {
            // Only supported for the platform trust manager.
            return null;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        mDelegate.checkServerTrusted(chain, authType);
        checkNotBlocked(chain, authType, null);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (mDelegate instanceof X509ExtendedTrustManager)
            ((X509ExtendedTrustManager) mDelegate).checkServerTrusted(chain, authType, socket);
        else
            mDelegate.checkServerTrusted(chain, authType);
        String host = null;
        if (socket instanceof SSLSocket)
            host = getPeerHost(((SSLSocket) socket).getHandshakeSession());
        checkNotBlocked(chain, authType, host);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (mDelegate instanceof X509ExtendedTrustManager)
            ((X509ExtendedTrustManager) mDelegate).checkServerTrusted(chain, authType, engine);
        else
            mDelegate.checkServerTrusted(chain, authType);
        String host = engine != null ? getPeerHost(engine.getHandshakeSession()) : null;
        checkNotBlocked(chain, authType, host);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        mDelegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (mDelegate instanceof X509ExtendedTrustManager)
            ((X509ExtendedTrustManager) mDelegate).checkClientTrusted(chain, authType, socket);
        else
            mDelegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (mDelegate instanceof X509ExtendedTrustManager)
            ((X509ExtendedTrustManager) mDelegate).checkClientTrusted(chain, authType, engine);
        else
            mDelegate.checkClientTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return mDelegate.getAcceptedIssuers();
    }

    /**
     * Checks the chain sent by the server and the chain the system actually used for validation.
     * The latter is needed, as the system might complete the chain with certificates that were not sent by the server (e.g. cached intermediates).
     */
    private void checkNotBlocked(X509Certificate[] chain, String authType, @Nullable String host) throws CertificateException {
        checkNotBlocked(chain);
        if (mDelegateExtensions != null) {
            List<X509Certificate> validatedChain = mDelegateExtensions.checkServerTrusted(chain, authType, host);
            if (validatedChain != null)
                checkNotBlocked(validatedChain.toArray(new X509Certificate[0]));
        }
    }

    @VisibleForTesting
    static void checkNotBlocked(X509Certificate[] chain) throws CertificateException {
        if (chain == null)
            return;
        for (X509Certificate certificate : chain) {
            if (isBlocked(certificate)) {
                BBLog.w(LOG_TAG, "Rejected certificate chain containing a blocked CA: " + certificate.getSubjectX500Principal().getName());
                throw new CertificateException("Certificate chain contains a blocked CA.");
            }
        }
    }

    @VisibleForTesting
    static boolean isBlocked(X509Certificate certificate) {
        try {
            byte[] spki = certificate.getPublicKey().getEncoded();
            String spkiHash = HexUtil.bytesToHex(MessageDigest.getInstance("SHA-256").digest(spki)).toLowerCase();
            return BLOCKED_SPKI_SHA256.contains(spkiHash);
        } catch (Exception e) {
            // If we cannot check it, we do not trust it.
            return true;
        }
    }

    @Nullable
    private static String getPeerHost(@Nullable SSLSession session) {
        return session != null ? session.getPeerHost() : null;
    }
}
