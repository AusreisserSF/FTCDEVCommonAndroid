package org.firstinspires.ftc.ftcdevcommon;

// For fatal errors that occur during logging.
public class AutonomousLoggingException extends AutonomousRobotException {

    public AutonomousLoggingException(String pTag, String pErrorMessage) {
        super(pTag, pErrorMessage);
    }
}
