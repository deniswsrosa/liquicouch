package com.github.liquicouch.test.changelogs;

import com.couchbase.client.java.Bucket;

import com.github.liquicouch.changeset.ChangeLog;
import com.github.liquicouch.changeset.ChangeSet;


/**
 * @author deniswsrosa
 */
@ChangeLog(order = "1")
public class LiquiCouchTestResource {

  @ChangeSet(author = "testuser", id = "test1", order = "1")
  public void testChangeSet() {
    System.out.println("invoked 1");
  }

  @ChangeSet(author = "testuser", id = "test2", order = "2")
  public void testChangeSet2() {
    System.out.println("invoked 2");
  }

  @ChangeSet(author = "testuser", id = "test5", order = "3")
  public void testChangeSet5(Bucket bucket) {
    System.out.println("invoked 5 with bucket=" +bucket.name());
  }

}
