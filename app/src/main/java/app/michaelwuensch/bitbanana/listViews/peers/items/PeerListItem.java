package app.michaelwuensch.bitbanana.listViews.peers.items;


import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.util.AliasManager;

public class PeerListItem implements Comparable<PeerListItem> {

    private String mAlias;
    private Peer mPeer;

    public PeerListItem(Peer peer) {
        mPeer = peer;
        mAlias = AliasManager.getInstance().getAlias(peer.getPubKey());
    }

    public String getAlias() {
        return mAlias;
    }

    public Peer getPeer() {
        return mPeer;
    }

    public boolean equalsWithSameContent(Object o) {
        if (!equals(o)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(PeerListItem o) {
        return this.mAlias.toLowerCase().compareTo(o.mAlias.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerListItem that = (PeerListItem) o;

        return mPeer.getPubKey().equals(that.getPeer().getPubKey());
    }

    @Override
    public int hashCode() {
        return mPeer.getPubKey().hashCode();
    }
}
