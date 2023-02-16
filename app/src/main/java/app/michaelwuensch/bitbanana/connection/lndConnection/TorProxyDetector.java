package app.michaelwuensch.bitbanana.connection.lndConnection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.grpc.HttpConnectProxiedSocketAddress;

public class TorProxyDetector implements io.grpc.ProxyDetector {
    final InetSocketAddress mProxyAddress;

    public TorProxyDetector(int proxyPort) {
        mProxyAddress = new InetSocketAddress("127.0.0.1", proxyPort);
    }

    @Override
    public HttpConnectProxiedSocketAddress proxyFor(SocketAddress targetAddress) {
        return HttpConnectProxiedSocketAddress.newBuilder()
                .setTargetAddress((InetSocketAddress) targetAddress)
                .setProxyAddress(mProxyAddress)
                .build();
    }
}
