# AntMonitor
This is a repository for AntMonitor - a VPN-based packet capturing
system for Android. For an overview of the project, please visit
the project
[website](http://athinagroup.eng.uci.edu/projects/antmonitor/).

* The core capabilities of AntMonitor to intercept and inspect outgoing
packets are made available as a library - see the
[Using the AntMonitor Library](#using-the-antmonitor-library) section.
* For getting started with the AntMonitor app and library development, see
[Running the AntMonitor App](#running-the-antmonitor-app).

### License
AntMonitor is licensed under
[GPLv2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html).

## 1. Using the AntMonitor Library
The packet interception and inspection capabilities of AntMonitor have
also been packaged as an Android library. If you wish to simply use
that capability and write your own app from scratch, please refer
to our other GitHub Project -
[AntMonitorExample](https://github.com/UCI-Networking-Group/AntMonitorExample).


## 2. Running the AntMonitor App
For the rest of the document we will refer to the directory to which
you have cloned the repo as `CODE_ROOT`.

* Download [Android Studio](https://developer.android.com/studio/)
* Download the [Android NDK](https://developer.android.com/ndk/downloads/index.html)
* Set the `sdk.dir` and the `ndk.dir` keys found in the
`CODE_ROOT/app/local.properties` file to point to your Android SDK and
Android NDK installation paths, respectively.
* Click the "Run App" button within Android Studio to run the app on a
connected Android device or emulator.
* If you get the error "Gradle sync failed: No toolchains found in the NDK toolchains folder for ABI with prefix: mips64el-linux-android", run
```bash
cd $ANDROID_SDK_ROOT/ndk-bundle/toolchains
ln -s aarch64-linux-android-4.9 mips64el-linux-android
ln -s arm-linux-androideabi-4.9 mipsel-linux-android
```

### Documentation
We provide three sets of Javadocs that you can refer to, depending on
how you want to use AntMonitor:

1. [An API overview of the AntMonitor library](https://uci-networking-group.github.io/AntMonitorExample/):
    refer to this if you only want to use the
   AntMonitor library as-is.
2. [Development Documentation of the AntMonitor library](https://uci-networking-group.github.io/AntMonitor/antmonitorlib):
    refer to this if you want to change/understand the internals of the
    AntMonitor library.
3. [Development Documentation of the AntMonitor app](https://uci-networking-group.github.io/AntMonitor/app/):
    refer to this if you want to change/understand the AntMonitor app.

If you need to update the documentation, refer to
[UPDATING_DOCS.md](UPDATING_DOCS.md).

### Citing AntMonitor
If you create a publication (including web pages, papers published by a
third party, and publicly available presentations) using the AntMonitor
app or the AntMonitor Library, please cite the corresponding paper as
follows:

```
@article{shuba2016antmonitor,
  title={AntMonitor: A System for On-Device Mobile Network Monitoring and its Applications},
  author={Shuba, Anastasia and Le, Anh and Alimpertis, Emmanouil and Gjoka, Minas and Markopoulou, Athina},
  journal={arXiv preprint arXiv:1611.04268},
  year={2016}
}
```

We also encourage you to provide us (<antmonitor.uci@gmail.com>) with a
link to your publication. We use this information in reports to our
funding agencies.