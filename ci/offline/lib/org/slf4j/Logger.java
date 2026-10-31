package org.slf4j;
public interface Logger { void info(String s, Object... a); void warn(String s, Object... a); void error(String s, Object... a); void debug(String s, Object... a); void info(String s); void warn(String s); void error(String s); void warn(String s, Throwable t); void error(String s, Throwable t); }
