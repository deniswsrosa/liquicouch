package com.github.liquicouch.test.changelogs;

import com.couchbase.client.java.Bucket;
import com.github.liquicouch.changeset.ChangeLog;
import com.github.liquicouch.changeset.ChangeSet;

/**
 * @author deniswsorsa
 */
@ChangeLog
public class SpringDataChangelog {
  @ChangeSet(author = "deniswsorsa", id = "spring_test4", order = "4")
  public void testChangeSet(Bucket bucket) {
    System.out.println("invoked  with bucket=" + bucket.name());
  }
}
