## Regtest Setup

A regtest setup will save you a lot of time as it allows you to test BitBanana in a simulated network.


### Create a network

The easiest way to create and manage simulated regtest networks is by using Polar.

1. Download [Polar][polar]
2. Setup a network in Polar with at least one LND or Core Lightning node.


### Remote control your simulated regtest LND nodes with BitBanana

To connect BitBanana with a simulated LND node:
1. Click on a LND node in Polar and navigate to the connect tab.
2. Select "LND Connect" in the sub tab.
3. Copy the connect string using the copy icon.
4. Open BitBanana
5. Paste the copied connect string in BitBanana on the connect remote node screen or on the general scan screen.
6. In BitBanana, tap on the currently active node name at the top of the home screen. Then choose "Manage...". Choose the freshly added node. Then tap on "Change data".
7. If you are using an Android Studio Emulator, replace the IP address with `10.0.2.2`. If you are using an android phone, replace the IP address with the IP address of the computer running polar.
8. Scroll down and make sure both Tor & Certificate verification are turned off.
9. Have fun testing!

[polar]: https://lightningpolar.com/
