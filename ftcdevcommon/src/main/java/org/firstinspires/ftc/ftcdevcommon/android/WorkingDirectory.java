package org.firstinspires.ftc.ftcdevcommon.android;

import android.os.Environment;

import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;

import java.io.File;

public class WorkingDirectory {
    private static final String TAG = "WorkingDirectory";
    private static final String teamDataPath;
    private static final boolean workingDirExists;

    static {
        File sdCardDir = Environment.getExternalStorageDirectory();
        teamDataPath = sdCardDir.getAbsolutePath() + "/FIRST/TeamData";
        File teamDataDir = new File(teamDataPath);
        workingDirExists = teamDataDir.exists();
    }

    public static String getWorkingDirectory() {
        if (!workingDirExists)
            throw new AutonomousRobotException(TAG, "Could not open working directory " + teamDataPath);
        return teamDataPath;
    }
}
