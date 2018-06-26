package com.github.liquicouch.test.changelogs;

import com.couchbase.client.java.Bucket;
import com.github.liquicouch.changeset.ChangeLog;
import com.github.liquicouch.changeset.ChangeSet;
import org.springframework.core.env.Environment;

@ChangeLog(order = "3")
public class EnvironmentDependentTestResource {
  @ChangeSet(author = "testuser", id = "Envtest1", order = "01")
  public void testChangeSet7WithEnvironment(Bucket bucket) {
    System.out.println("invoked Envtest1 with bucket=" + bucket.name() );
  }
}
