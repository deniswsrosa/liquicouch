package com.github.liquicouch;

import com.couchbase.client.java.Bucket;
import com.github.liquicouch.changeset.ChangeEntry;
import com.github.liquicouch.dao.BucketWrapper;
import com.github.liquicouch.dao.ChangeEntryDAO;
import com.github.liquicouch.resources.EnvironmentMock;
import com.github.liquicouch.test.changelogs.EnvironmentDependentTestResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class LiquiCouchEnvTest {

  @Mock
  private Bucket bucket;

  @Mock
  private BucketWrapper bucketWrapper;

  private LiquiCouch runner;

  @Before
  public void init() throws Exception {
    runner = new LiquiCouch(bucket);
    ChangeEntryDAO dao = new ChangeEntryDAO(bucket);
    dao.setBucketWrapper(bucketWrapper);
    runner.setDAO(dao);
    runner.setEnabled(true);
  }

  @Test
  public void shouldRunChangesetWithEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);
    // when
    runner.execute();
    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Envtest1"));

  }

  @Test
  public void shouldRunChangesetWithNullEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Envtest1"));
  }
}
