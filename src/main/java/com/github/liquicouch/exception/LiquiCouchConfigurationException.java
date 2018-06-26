package com.github.liquicouch.exception;

/**
 * @author deniswsrosa
 */
public class LiquiCouchConfigurationException extends LiquiCouchException {
  public LiquiCouchConfigurationException(String message) {
    super(message);
  }

  public LiquiCouchConfigurationException(String message, Throwable e) {
    super(message, e);
  }
}
