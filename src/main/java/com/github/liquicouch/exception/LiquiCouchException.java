package com.github.liquicouch.exception;

/**
 * @author deniswsrosa
 */
public class LiquiCouchException extends Exception {
  public LiquiCouchException(String message) {
    super(message);
  }

  public LiquiCouchException(String message, Throwable cause) {
    super(message, cause);
  }
}
