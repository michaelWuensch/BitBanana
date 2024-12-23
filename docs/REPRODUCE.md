# Reproduce

## Why is this important?

If an application available on the app store cannot be reproduced by using the given source code, it poses a significant security threat. In such cases, the developer could potentially add hidden code and vulnerabilities that may allow them to gain unauthorized access to sensitive user data or even steal their funds.


## Verify Github release
Download or clone the source code from github.  
To verify the release files distributed with github releases, first build the APK:
```
$ cd BitBanana/
$ git checkout v0.8.8
$ docker run --rm -v `pwd`:/project mingc/android-build-box:1.28.0 bash -c 'cd /project; ./gradlew assembleRelease'
```
After executing this command, the built APK can be found in this directory:
.../BitBanana/app/build/outputs/apk/release/bitbanana-0.8.8_65-release-unsigned.apk

Extract both, the just built apk and the apk downloaded from github release.
Rename the folder of the created apk to "built", the other one to "official".

Then run the following command:

```
$ diff --brief --recursive built/ official/
```

If this command lists any files, this means that some of the files are not identical which in turn means the build cannot be verified. If it does not list any differences, the build is verified!

## Verify Play Store release
Verifying a Play Store release unfortunatelly got more complicated since Google enforces usage of Android App Bundles (AAB).  
To verify the release files distributed on Play Store, first build the AAB:
```
$ cd BitBanana/
$ git checkout v0.8.8
$ docker run --rm -v `pwd`:/project mingc/android-build-box:1.28.0 bash -c 'cd /project; ./gradlew bundleRelease'
```
After executing this command, the build AAB can be found in this directory:
.../BitBanana/app/build/outputs/bundle/release/bitbanana-0.8.8_65-release.aab

The AAB file is used to create device specific APK files.
When you download the app from Google Play, google is dynamically providing the APK that fits your phone. This APK will be different from the universal APK distributed on GitHub releases. To be able to verify the Google Play download we need to extract the same device specific APK from the AAB we just created.  
To do so, we need the [bundletool][bundletool].  
Connect your smartphone to the PC and then run:

```
$ bundletool build-apks --connected-device --bundle=bitbanana-0.8.8_65-release.aab --output=bitbanana-0.8.8_65-release.apks
```

Good, we now have a self built set of device specific APKs in form of an .apks file.
We can simply unzip this file. After doing that, in the "splits" folder there are a few apks.
Create a folder named "built" and copy the content of the split folder into it.  

Next we need to grab the APKs that are delivered by Google Play.
To get hold of them, install BitBanana from the Play Store on the same smartphone that you used to extract the APK from the AAB.
Then execute the following commands:

```
$ bundleId="app.michaelwuensch.bitbanana"
$ apks=`adb shell pm path $bundleId`
$ for apk in $apks; do adb pull `echo $apk | awk '{print $NF}' FS=':' | tr -d '\r\n'`; done
```
Create a folder called "official" and move all APKs that were copied by the last command into it.

We now have the two folders "built" and "official", each one containing 3 APKs. For our diff command to be useful we have to extract all those apks inside those folders.
Unfortunatelly the names of the self built APKs do not match those from the officially pulled APKs. Therefore we have to make sure that after unzipping an APK we rename the folder in a way that the names match on both, the "built" and the "official" version.
For example the folder that was created by unzipping the APK split that depends on dpi could be named "dpi" in both cases and so on.
After unzipping the apks, delete them.

Now that we finally have two folders "built" and "official", each one with 3 subfolders with identical names, we can run the diff command:

```
$ diff --brief --recursive built/ official/
```

This command should list as few differences as possible. In fact it should only list differences that have to do with signing & repackaging by google.

- AndroidManifest.xml
- META-INF/BNDLTOOL.RSA
- META-INF/BNDLTOOL.SF
- META-INF/MANIFEST.MF
- stamp-cert-sha256

If this command lists any other files, this means that some of the files are not identical which in turn means the build cannot be verified. Otherwhise the build is verified!

Please note:
The differences are not always the same. It seems to depend on which smartphone you use for the process. And on some google seems to even mess with dex files, which means it is hard to call it reproducible in that case. Still this procedure is the best I could come up with so far to reproduce a Playstore Release using Android App Bundles.

If you know any better or easier way to verify such a build, please let me know! :)

[bundletool]: https://github.com/google/bundletool