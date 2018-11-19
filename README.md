# AntMonitor
This is a repository for AntMonitor - a VPN-based packet capturing
system for Android. For an overview of the project, please visit
the project
[website](http://athinagroup.eng.uci.edu/projects/antmonitor/).

### Running the AntMonitor App
For the rest of the document we will refer to the directory to which
you have cloned the repo as `CODE_ROOT`.

* Download [Android Studio](https://developer.android.com/studio/)
* Download the [Android NDK](https://developer.android.com/ndk/downloads/index.html)
* Set the `sdk.dir` and the `ndk.dir` keys found in the
`CODE_ROOT/app/local.properties` file to point to your Android SDK and
Android NDK installation paths, respectively.
* Click the "Run App" button within Android Studio to run the app on a
connected Android device or emulator.


### Using the AntMonitor Library
The packet interception and inspection capabilities of AntMonitor have
also been packaged as an Android library. If you wish to simply use
that capability and write your own app from scratch, please refer
to our other GitHub Project -
[AntMonitorExample](https://github.com/UCI-Networking-Group/AntMonitorExample).


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