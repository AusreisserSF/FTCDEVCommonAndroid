# FTCDEVCommonAndroid
Common classes for development in Android Studio

This project is the home of the utilities that are common to all Android Studio projects.

This project builds an aar file (with a version number) that can then be loaded into any Android Studio project.
There is an analagous project for IntelliJ. The two projects both have copies of the same source code - and so must be kept in sync -
except for the subdirectories intellij and android, each of which contains platform-specific code.

How to build the aar file:
Make the source code changes
In the build.gradle file change versionName '1_5' to the correct version
Build the project
Separately select Build -> Make module FTCDEVCommonAndroid.ftcdevcommon to build the aar
The aar should appear in C:\FTCDevCommonAndroid\ftcdevcommon\build\outputs\aar
But .gitignore includes build/ so instead of fighting with trying to ignore everything in
 build while keeping \build\outputs\aar, just force commit and push the aar from the Git
 command line with git add --force ftcdevcommon_1_5-debug.aar


