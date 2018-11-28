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

### Documentation
The Javadoc sits in the `documentation` directory of the repo and is
also available in web form
[here](https://uci-networking-group.github.io/AntMonitor/).

#### Generating Documentation
To generate the documentation you can use Android Studio:
* Go to Tools -> Generate Javadoc
* Select the desired scope and view
* To add a custom overview page, add the following to the command-line
arguments: `-overview path_to/overview.html --allow-script-in-comments`
* Copy any resources (e.g. `src/javadoc-resources/release`) to the output folder

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