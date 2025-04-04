# Verifying a release

Starting with version v0.9.4, BitBanana includes a signature file (.asc) to verify the authenticity of the released APK.
Before you can verify a release, you'll need to have `gpg` or `gpg2` installed on your system.

## Import the public signing key (one-time setup)

First, import the public key that is used to sign all releases. This only needs to be done once — future releases can be verified without repeating this step.


```
curl -s https://github.com/michaelWuensch.gpg | gpg --import
```

## Verify
You're all set! Just make sure the `.apk` and `.asc ` file are in your current folder, and swap out x.x.x with the actual version:

```
gpg --verify bitbanana-x.x.x-release.apk.asc bitbanana-x.x.x-release.apk
```

You should see the following if the verification was successful:

```
gpg: Signature made XXX
gpg:                using RSA key 9D6DC7616BC54E2724EF016350D945755565DB4B
gpg: Good signature from "Michael Wünsch <michael90@protonmail.com>"
```
Where XXX will be the date when the release was signed.