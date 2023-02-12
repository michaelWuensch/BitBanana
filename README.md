# BitBanana

![Screenshot of BitBanana app](docs/screenshot.png)

Native android lightning wallet for node operators focused on user experience and ease of use ⚡️

BitBanana is not a classical wallet, it is actually a remote control for one or more of your LND Nodes. BitBanana aims to give node operators the opportunity to use their node in everyday situations with a simple and appealing user interface.

The easiest way to get started is using [Umbrel](https://getumbrel.com/) to run LND on a raspberry pi and then connect BitBanana to that node.

BitBanana forked from Zap Android. You can find more information about this on the [Rebranding](docs/REBRANDING.md) page.

## Features
- [x] 100% free, only bitcoin network fees apply. BitBanana does not earn a single sat and has no plans to monetize in the future.

**Wallet/Lightning**
- [x] Connect to remote Lnd nodes
- [x] Manage multiple nodes
- [x] Channel Management
- [x] Routing summary
- [x] Contacts
- [x] Fiat currency prices
- [x] Support for Bech32 and P2SH addresses
- [x] BTC, mBTC, bit & Satoshi units
- [x] Available in many languages
- [x] LNURL support (pay, withdraw & channel)
- [x] Send funds without an invoice (keysend)
- [x] Send funds to lightning addresses (email like addresses)
- [x] Transaction filter
- [x] Read NFC tags
- [x] [Avatars](https://github.com/michaelWuensch/avathor-rfc#avathor) 
- [x] Bitcoin only, no shitcoins!

**Security & Privacy**
- [x] 100% Non-custodial
- [x] Tor support
- [x] PIN protected access
- [x] Scrambled PIN by default
- [x] Protection against screen recording
- [x] Option to hide total balance
- [x] User guardian system (BitBanana warns users when they are about to perform potentially dangerous or privacy leaking actions)


## Security

If you discover or learn about a potential error, weakness, or threat that can compromise the security of BitBanana, we ask you to keep it confidential and [submit your concern directly to the BitBanana developer](mailto:bitbananasecurity@proton.me?subject=[GitHub]%20BitBanana%20Security).

## Non-custodial

Bitbanana is fully non-custodial. When using the app there is absolutely no interaction with any team or service. We do not even know you are using our software.

## Get Help

If you are having problems with BitBanana, please report the issue in [GitHub][issues] or on [discord][discord] with screenshots and/or how to reproduce the bug/error.



## Contribute

Hey! Do you like BitBanana? Awesome! We could actually really use your help!

Open source isn't just writing code. BitBanana could use your help with any of the following:

- [Translating](docs/TRANSLATING.md) the app
- Finding (and reporting!) bugs
- New feature suggestions
- Answering questions on issues
- Documentation improvements
- Reviewing pull requests
- Helping to manage issue priorities
- Fixing bugs/new features

If any of that sounds cool to you, feel free to dive in! [Open an issue][issues] or submit a pull request.

If you would like to help contribute to the project, please see the [Contributing Guide](docs/CONTRIBUTING.md)

If you want to setup a testing environment, please see the [Regtest Guide](docs/REGTEST.md)

And if you want to build the app yourself take a look at the [Installation Guide](docs/INSTALL.md)

## Maintainers
- [Michael Wünsch](https://github.com/michaelWuensch)

## License

This project is open source under the MIT license, which means you have full access to the source code and can modify it to fit your own needs. See [LICENSE](LICENSE) for more information.

[MIT](LICENSE) © Jack Mallers, Michael Wuensch

[issues]: https://github.com/michaelWuensch/BitBanana/issues
[discord]: https://discord.gg/Xg85BuTc9A