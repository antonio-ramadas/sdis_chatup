package chatup.main;

public class ChatupGlobals {

    public static final String DefaultEncoding = "utf-8";
    public static final String HeartbeatServiceUrl = "HeartbeatService";
    public static final String MessageServiceUrl = "MessageService";
    public static final String UserServiceUrl = "UserService";
    public static final String RoomServiceUrl = "RoomService";
    private static final String fmtAbort = "Exception caught: %s on method %s@%s!";
    private static final String fmtUsage = "Program usage: %s %s";
    
    public static final int DefaultTimeout = 5000;
    public static final int DefaultCacheSize = 100;
    public static final int DefaultBuffer = 4096;

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

    static void abort(final Exception paramException) {

        final StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[2];

        final String abortMessage = String.format(
            fmtAbort,
            paramException.getClass().getSimpleName(),
            stackTrace.getMethodName(),
            stackTrace.getClassName()
        );

        System.out.println(abortMessage);
        System.exit(1);
    }
}