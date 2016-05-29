package chatup.main;

import chatup.server.ServerType;

import com.esotericsoftware.minlog.Log;

import java.io.File;

public class ChatupGlobals {

    public static final String DefaultEncoding = "utf-8";
    public static final String HeartbeatServiceUrl = "/HeartbeatService";
    public static final String MessageServiceUrl = "/MessageService";
    public static final String UserServiceUrl = "/UserService";
    public static final String RoomServiceUrl = "/RoomService";

    private static final String fmtAbort = "Caught exception %s on method %s@%s!";
    private static final String fmtUsage = "Invalid arguments, usage: %s %s";

    static final String keystoreFilename = "chatup.jks";
    static final String keystorePassword = "sdis-feup";
    
    public static final int DefaultTimeout = 5000;
    public static final int DefaultCacheSize = 100;
    public static final int DefaultBuffer = 32768;

    static final int DefaultHttpPort = 8080;
    static final int DefaultTcpPort = 8085;

    static void usage(final String paramArguments) {

        final StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[2];

        final String usageMessage = String.format(
            fmtUsage,
            stackTrace.getClassName(),
            paramArguments
        );

        System.out.println(usageMessage);
        System.exit(1);
    }

    public static  boolean createDirectory(final String paramDirectory) {
        final File myFile = new File(paramDirectory);
        return !(!myFile.exists() || !myFile.isDirectory()) || myFile.mkdir();
    }

    public static void abort(final ServerType serverType, final Exception paramException) {

        final StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[2];

        final String abortMessage = String.format(
            fmtAbort,
            paramException.getClass().getSimpleName(),
            stackTrace.getMethodName(),
            stackTrace.getClassName()
        );

        Log.error(serverType.toString(), abortMessage);
        System.exit(1);
    }

    public static void warn(final String paramService, final Exception paramException) {

        final StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[2];

        final String abortMessage = String.format(
            fmtAbort,
            paramException.getClass().getSimpleName(),
            stackTrace.getMethodName(),
            stackTrace.getClassName()
        );

        Log.error(paramService, abortMessage);
    }
}