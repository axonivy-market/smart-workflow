package ch.ivyteam.test.log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import ch.ivyteam.util.threadcontext.IvyThreadContext;

/**
 * Usage:
 * <code><pre>@RegisterExtension
 * LoggerAccess log = new LoggerAccess("ch.ivyteam.ivy.deprecation.feature");
 *
 * assertThat(log.warnings())
 * .hasSize(1)
 * .contains("log is using the deprecated my-feature.");</pre></code>
 */
public class LoggerAccess implements BeforeEachCallback, AfterEachCallback {
  private static final Predicate<LogEvent> ALL = logEvent -> true;
  private final Logger logger;
  private final LogAppender appender;

  public LoggerAccess() {
    this(LogManager.getRootLogger().getName());
  }

  public LoggerAccess(String logger) {
    this(logger, Level.ALL);
  }

  public LoggerAccess(Level level) {
    this(LogManager.getRootLogger().getName(), level);
  }

  public LoggerAccess(String logger, Level level) {
    this.logger = LogManager.getLogger(logger);
    Configurator.setLevel(logger, level);
    this.appender = new LogAppender();
  }

  public List<String> all() {
    return filter(ALL);
  }

  public List<String> fatals() {
    return filter(LogEvent::isFatal);
  }

  public List<String> errors() {
    return filter(LogEvent::isError);
  }

  public List<String> warnings() {
    return filter(LogEvent::isWarning);
  }

  public List<String> infos() {
    return filter(LogEvent::isInfo);
  }

  public List<String> problems() {
    return filter(LogEvent::isProblem);
  }

  public List<String> debugs() {
    return filter(LogEvent::isDebug);
  }

  public List<String> traces() {
    return filter(LogEvent::isTrace);
  }

  public List<LogEvent> events() {
    return appender.logEvents.stream().collect(Collectors.toList());
  }

  public String log() {
    return appender.logEvents
        .stream()
        .map(LogEvent::toString)
        .collect(Collectors.joining("\n"));
  }

  public void clear() {
    appender.logEvents.clear();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    appender.start();
    appender.addTo(logger);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    appender.removeFrom(logger);
    appender.stop();
  }

  private List<String> filter(Predicate<LogEvent> filter) {
    return appender.logEvents.stream()
        .filter(filter)
        .map(LogEvent::message)
        .collect(Collectors.toList());
  }

  public static class LogAppender extends AbstractAppender {

    private final Collection<LogEvent> logEvents = new ConcurrentLinkedDeque<>();

    public LogAppender() {
      super("Test Log Appender", null, null, true, null);
    }

    @Override
    public void append(org.apache.logging.log4j.core.LogEvent event) {
      logEvents.add(new LogEvent(event));
    }

    @Override
    public void stop() {
      super.stop();
      logEvents.clear();
    }

    public Collection<LogEvent> events() {
      return logEvents;
    }

    public void addTo(Logger logger) {
      ((org.apache.logging.log4j.core.Logger) logger).addAppender(this);
    }

    public void removeFrom(Logger logger) {
      ((org.apache.logging.log4j.core.Logger) logger).removeAppender(this);
    }
  }

  public static class LogEvent {

    private final Map<String, String> threadContext = new HashMap<>();
    private final org.apache.logging.log4j.core.LogEvent event;

    private LogEvent(org.apache.logging.log4j.core.LogEvent event) {
      this.event = event;
      for (var threadLocal : IvyThreadContext.getIvyThreadLocals()) {
        if (threadLocal.get() != null) {
          threadContext.put(threadLocal.getName(), threadLocal.toFormatedValue());
        }
      }
    }

    private boolean isWarning() {
      return event.getLevel() == Level.WARN;
    }

    public boolean isError() {
      return event.getLevel() == Level.ERROR;
    }

    private boolean isFatal() {
      return event.getLevel() == Level.FATAL;
    }

    public boolean isInfo() {
      return event.getLevel() == Level.INFO;
    }

    public boolean isDebug() {
      return event.getLevel() == Level.DEBUG;
    }

    public boolean isTrace() {
      return event.getLevel() == Level.TRACE;
    }

    public boolean isProblem() {
      return isWarning() || isError() || isFatal();
    }

    public String message() {
      return event.getMessage().getFormattedMessage();
    }

    protected Level level() {
      return event.getLevel();
    }

    public Throwable thrown() {
      return event.getThrown();
    }

    public Map<String, String> context() {
      return event.getContextData().toMap();
    }

    public Map<String, String> ivyThreadContext() {
      return threadContext;
    }

    @Override
    public String toString() {
      return "[" + event.getLevel() + "] " + message();
    }
  }
}
