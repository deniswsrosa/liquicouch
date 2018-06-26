package com.github.liquicouch.test.changelogs;

import com.couchbase.client.java.Bucket;
import com.github.liquicouch.changeset.ChangeLog;
import com.github.liquicouch.changeset.ChangeSet;

/**
 * @author deniswsrosa
 */
@ChangeLog(order = "2")
public class LiquiCouchChange2TestResource {

  @ChangeSet(author = "testuser", id = "Btest1", order = "1")
  public void testChangeSet(){
    System.out.println("invoked B1");
  }
  @ChangeSet(author = "testuser", id = "Btest2", order = "2")
  public void testChangeSet2(){
    System.out.println("invoked B2");
  }

  @ChangeSet(author = "testuser", id = "Btest3", order = "3")
  public void testChangeSet6(Bucket bucket) {
    System.out.println("invoked B3 with bucket=" +bucket.name());
  }

}
