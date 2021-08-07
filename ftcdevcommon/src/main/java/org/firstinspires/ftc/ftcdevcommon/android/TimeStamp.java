package org.firstinspires.ftc.ftcdevcommon.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeStamp {

    // Requires Android minSdkVersion 26
    // public static String getLocalDateTimeStamp(LocalDateTime pLocalDateTime) {
    //    return pLocalDateTime.format(DateTimeFormatter.ofPattern("MMddHHmm'_'ssSSS"));
    // }

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmm'_'ssSSS", Locale.US);
    public static String getDateTimeStamp(Date pDate) {
        return dateFormat.format(pDate);
    }

}
