package edu.harvard.seas.synthesis.logging;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.harvard.seas.synthesis.HTTPServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.*;

public class SynthesisLogger {

    public static boolean isLoggingEnabled = false; // disable logging by default

    private static SynthesisLogger _synthesisLogger;

    private final Logger logger = Logger.getLogger(SynthesisLogger.class.getName());
    private FileHandler fileHandler = null;
    private SimpleFormatter formatter = null;

    public static SynthesisLogger getSynthesisLogger() {
        if(_synthesisLogger == null){
            _synthesisLogger = new SynthesisLogger();
        }

        return _synthesisLogger;
    }

    private SynthesisLogger(){
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);

        // Setting custom formatter
        formatter = new SimpleFormatter() {
            private static final String format = "$$$ [%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        };

        try {
            String currentDirectory = System.getProperty("user.dir");
            Path logDirectory = Paths.get( currentDirectory, "/instrument_log/").toAbsolutePath();
            Files.createDirectories(logDirectory);
            String logfile_path = Paths.get(logDirectory.toAbsolutePath().toString(), "synthesizer_log_" + HTTPServer.session_id + ".log").toString();
            fileHandler = new FileHandler(logfile_path, true);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (Exception e){
            System.out.println(e.getStackTrace());
        }
    }

    private boolean canLog() {
        return isLoggingEnabled && (fileHandler != null);
    }

    public void logString(String message){
        if(!canLog()) {
            return;
        }
        try {
            LogRecord lr = new LogRecord(Level.INFO, MessageFormat.format("{0}\r\n", message));
            logger.log(lr);
        }
        catch (Exception e){
            logException(e);
        }
    }

    public void logException(Exception e){
        if(!canLog()) {
            return;
        }
        try {
            LogRecord lr = new LogRecord(Level.SEVERE, MessageFormat.format("{0}\r\n", e.getStackTrace()));
            logger.log(lr);
        }
        catch (Exception exp){
            System.out.println(exp.getStackTrace());
        }
    }

    public void logObject(Object obj) {
        if(!canLog()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            LogRecord lr = new LogRecord(Level.INFO, MessageFormat.format("{0}\r\n", mapper.writeValueAsString(obj)));
            logger.log(lr);
        }
        catch (Exception e){
            logException(e);
        }
    }

    public void logObjectWithMessage(String message, Object obj) {
        if(!canLog()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            LogRecord lr = new LogRecord(Level.INFO, MessageFormat.format("{0} {1}\r\n", message, mapper.writeValueAsString(obj)));
            logger.log(lr);
        }
        catch (Exception e){
            logException(e);
        }
    }
}
