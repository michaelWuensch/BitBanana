package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import app.michaelwuensch.bitbanana.models.Balances;

public class BalancesTest {

    @Test
    public void testBalanceTotal() {
        long onChainBalanceConfirmed = 200;
        long onChainBalanceUnconfirmed = 200;
        long channelBalance = 100;
        long channelBalancePendingOpen = 100;
        long channelBalanceLimbo = 100;

        Balances balances = new Balances.Builder()
                .setOnChainConfirmed(onChainBalanceConfirmed)
                .setOnChainUnconfirmed(onChainBalanceUnconfirmed)
                .setChannelBalance(channelBalance)
                .setChannelBalancePendingOpen(channelBalancePendingOpen)
                .setChannelBalanceLimbo(channelBalanceLimbo)
                .build();

        assertEquals(700, balances.total());
        assertEquals(400, balances.onChainTotal());
    }
}
