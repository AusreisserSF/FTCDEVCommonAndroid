# FTCDEVCommonAndroid
Common classes for FTC development in Android Studio

This project is the home of the utilities that are common to all Android Studio projects for FTC.

This project builds an aar file (with a version number) that can then be loaded into any Android Studio project.  

There is an analagous project for IntelliJ. The two projects both have copies of the same source code - and so must be kept in sync -
except for the subdirectories intellij and android, each of which contains platform-specific code.

The FTCDEVCommonAndroid library may be incorporated into an FTC Android Studio project either as a source code module or as an aar.  

1. As a source code module
On Windows, for example, download and expand the zip file for this repository into a temporary directory, e.g. C:\TEMP\FTCDEVCommonAndroid  
Open your FTC Android Studio project and select File > New > Import Module  
Select the source directory, e.g. C:\TEMP\FTCDEVCommonAndroid\ftcdevcommon and make sure the module name is :ftcdevcommon  
Click Finish  
In your Android Studio project, go to File > Project Structure and click on Dependencies, then TeamCode  
Under the Declared Dependencies heading, click the + sign  
Select 3 Module Dependency and click the checkbox for ftcdevcommon  
Click OK  

2. As an aar  
On Windows, for example, download and expand the zip file for this repository into a temporary directory, e.g. C:\TEMP\FTCDEVCommonAndroid   
In your Android Studio project, go to File > Project Structure and click on Dependencies, then TeamCode  
Under the Declared Dependencies heading, click the + sign  
Select 2 JAR/AAR Dependency and, in the box "Add Jar/Aar Dependency -> "Provide a path to the library file or directory to add", navigate
   (if possible) to the aar file, e.g. C:\FTCDEVCommonAndroid\ftcdevcommon\build\outputs\aar\ftcdevcommon_2_0-debug.aar;
   replace 2_0 with the target version number. If a file system browser does not appear then you will have to copy-and-paste the full path
   and file name.
Click OK

If you copy the source code and then add your own utilities to it, here's how to build the aar file:  
Make the source code changes  

In the build.gradle file change versionName 'n_v' to the correct version, e.g. 1_5 -> 2_0  

Build the project  

Separately select Build -> Make module FTCDEVCommonAndroid.ftcdevcommon to build the aar  

The aar will appear in C:\FTCDevCommonAndroid\ftcdevcommon\build\outputs\aar.
But .gitignore includes build/ so instead of fighting with trying to ignore everything in
build while keeping \build\outputs\aar, take the following steps:  
1. Open Git bash in the aar folder and force add it via $ git add --force ftcdevcommon_n_v-debug.aar (with the correct version number).
2. Open the Android Studio project, commit and push


