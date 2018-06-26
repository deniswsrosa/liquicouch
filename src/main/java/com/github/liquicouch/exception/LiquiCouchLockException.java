package com.github.liquicouch.exception;

/**
 * Error while can not obtain process lock
 */
public class LiquiCouchLockException extends LiquiCouchException {
  public LiquiCouchLockException(String message) {
    super(message);
  }
}
