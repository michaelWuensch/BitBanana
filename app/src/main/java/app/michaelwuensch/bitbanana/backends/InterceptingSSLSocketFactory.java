package app.michaelwuensch.bitbanana.backends;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import app.michaelwuensch.bitbanana.util.BBLog;

/**
 * This class is used to intercept the TLS handshake of the connection and extract the certificate.
 */
public class InterceptingSSLSocketFactory extends SSLSocketFactory {
    private static final String LOG_TAG = InterceptingSSLSocketFactory.class.getSimpleName();
    private final SSLSocketFactory delegate;

    public InterceptingSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    private Socket intercept(Socket socket) throws IOException {
        if (socket instanceof SSLSocket sslSocket) {
            BBLog.d(LOG_TAG, "Supported protocols: " + Arrays.toString(sslSocket.getSupportedProtocols()));
            BBLog.d(LOG_TAG, "Enabled protocols: " + Arrays.toString(sslSocket.getEnabledProtocols()));
            //BBLog.d(LOG_TAG, "Supported cipher suites: " + Arrays.toString(sslSocket.getSupportedCipherSuites()));
            //BBLog.d(LOG_TAG, "Enabled cipher suites: " + Arrays.toString(sslSocket.getEnabledCipherSuites()));

            sslSocket.addHandshakeCompletedListener(event -> {
                try {
                    BBLog.d(LOG_TAG, "Negotiated protocol: " + event.getSession().getProtocol());
                    BBLog.d(LOG_TAG, "Negotiated Cipher suite: " + event.getCipherSuite());
                    Certificate[] certs = event.getPeerCertificates();
                    if (certs.length > 0 && certs[0] instanceof X509Certificate cert) {
                        CertificateInfoStore.setServerCertificate(cert);
                    }
                } catch (SSLPeerUnverifiedException e) {
                    BBLog.w(LOG_TAG, "Intercepting TLS handshake failed: " + e.getMessage());
                }
            });
        }
        return socket;
    }

    // Delegate and wrap all createSocket methods
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return intercept(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return intercept(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return intercept(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return intercept(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return intercept(delegate.createSocket(address, port, localAddress, localPort));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}
