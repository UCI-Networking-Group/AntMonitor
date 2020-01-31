## Generating Documentation
To generate the documentation, you can use Android Studio:
 * Go to Tools -> Generate Javadoc.
 * Select "Custom Scope" as the scope.
 * Click the elipses next to "Custom Scope," and add a new scope by
 clicking the green plus button.
 * Select "Local" and name the scope as you wish for future uses.

Next, depending on the type of documentation you want to generate, select
the rest of the settings as follows.
* For an
[API overview](https://uci-networking-group.github.io/AntMonitorExample/)
of the AntMonitor library:

    * Expand "Production Classes," then expand "antmonitorlib" and highlight
    the 4 directories that start with "edu.uci.calit2.antmonitor.lib."
    Next, click "Include Recursively," and when the process is done,
    click "Apply" and then "OK."
    * Select an output directory. Note that this is usually the "docs"
    folder inside the
    [AntExample project](https://github.com/UCI-Networking-Group/AntMonitorExample/).
    * In the menu below "Output directory," select "protected."
    This will only generate documentation for classes with "protected" or
    "public" visibility.
    * In "Other command line arguments," add the following:
    ``-overview CODE_ROOT/antmonitorlib/overview.html --allow-script-in-comments``
    * Keep the rest of the settings at default and click OK.
    * If generating fresh documentation (not from the cloned AntExample project),
    then you will also have to copy
    ``CODE_ROOT/antmonitorlib/src/javadoc-resources/release``
    to the generated documentation folder.

* For the
[development documentation](https://uci-networking-group.github.io/AntMonitor/antmonitorlib)
of the AntMonitor library:

    * Expand "Production Classes," then expand "antmonitorlib" and highlight
    the 4 directories that start with "edu.uci.calit2.antmonitor.lib."
    Next, click "Include Recursively," and when the process is done,
    click "Apply" and then "OK."
    * Select an output directory. Note that this is usually the
    ``CODE_ROOT/docs/antmonitorlib`` directory.
    * In the menu below "Output directory," select "private." This will
    generate documentation for all classes.
    * In "Other command line arguments," add the following:
    ``-overview CODE_ROOT/antmonitorlib/overview-dev.html --allow-script-in-comments``
    * Keep the rest of the settings at default and click OK.
    * If generating fresh documentation (not from this cloned repo),
    then you will also have to copy
    ``CODE_ROOT/antmonitorlib/src/javadoc-resources/dev``
    to the generated documentation folder.

* For the
[development documentation](https://uci-networking-group.github.io/AntMonitor/app/)
of the AntMonitor app:
    * Expand "Production Classes," then expand "app" and select the
    "edu.uci.calit2.anteater" folder.
    Next, click "Include Recursively," and when the process is done,
    click "Apply" and then "OK."
   * Select an output directory. Note that this is usually the
    ``CODE_ROOT/docs/app`` directory.
    * In the menu below "Output directory," select "private." This will
    generate documentation for all classes.
    * In "Other command line arguments," add the following:
    ``-overview CODE_ROOT/app/overview.html --allow-script-in-comments``
    * Keep the rest of the settings at default and click OK.
    * If generating fresh documentation (not from this cloned repo),
    then you will also have to copy
    ``CODE_ROOT/app/src/main/javadoc-resources``
    to the generated documentation folder.

**Note**: after re-generating documentation, you may need to re-apply
the copyright statement to the newly generated docs (see next section).

## Updating Copyright
In Android Studio:
* Open Settings (Ctrl+Alt+S)
* Go to Editor -> Copyright -> Copyright Profiles
* Click the green plus button to add a profile and fill out the content accordingly
* Click on Editor -> Copyright, select your new profile as the default, and click OK
* Go to Code -> Update Copyright, select the scope as you wish, and click OK
* Copyright is now updated