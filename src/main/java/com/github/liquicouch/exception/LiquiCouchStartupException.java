package com.github.liquicouch.exception;

public class LiquiCouchStartupException extends RuntimeException {
  public LiquiCouchStartupException(String message, Throwable t){
    super(message, t);
  }

  public LiquiCouchStartupException(String message){
    super(message);
  }
}
