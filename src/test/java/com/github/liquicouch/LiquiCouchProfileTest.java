package com.github.liquicouch;

import com.couchbase.client.java.Bucket;
import com.github.liquicouch.changeset.ChangeEntry;
import com.github.liquicouch.dao.BucketWrapper;
import com.github.liquicouch.dao.ChangeEntryDAO;
import com.github.liquicouch.resources.EnvironmentMock;
import com.github.liquicouch.test.changelogs.LiquiCouchChange2TestResource;
import com.github.liquicouch.test.profiles.def.UnProfiledChangeLog;
import com.github.liquicouch.test.profiles.dev.ProfiledDevChangeLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for Spring profiles integration
 *
 * @author deniswsrosa
 */
@RunWith(MockitoJUnitRunner.class)
public class LiquiCouchProfileTest {

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
  public void shouldRunDevProfileAndNonAnnotated() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev", "test"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Pdev1"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Pdev4"));
    verify(bucketWrapper, times(0)).insert(new ChangeEntry("Pdev3"));

  }

  @Test
  public void shouldRunUnprofiledChangeLog() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("test"));
    runner.setChangeLogsScanPackage(UnProfiledChangeLog.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Pdev1"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Pdev2"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Pdev3"));
    verify(bucketWrapper, times(0)).insert(new ChangeEntry("Pdev4"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Pdev5"));
  }

  @Test
  public void shouldNotRunAnyChangeSet() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("foobar"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(0)).insert(new ChangeEntry("Pdev1"));
    verify(bucketWrapper, times(0)).insert(new ChangeEntry("Pdev2"));
    verify(bucketWrapper, times(0)).insert(new ChangeEntry("Pdev3"));
    verify(bucketWrapper, times(0)).insert(new ChangeEntry("Pdev4"));
  }



  @Test
  public void shouldRunChangeSetsWhenNoEnv() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(LiquiCouchChange2TestResource.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest1"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest2"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest3"));
  }


  @Test
  public void shouldRunChangeSetsWhenEmptyEnv() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(LiquiCouchChange2TestResource.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest1"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest2"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest3"));
  }



  @Test
  public void shouldRunAllChangeSets() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev"));
    runner.setChangeLogsScanPackage(LiquiCouchChange2TestResource.class.getPackage().getName());
    when(bucket.get(any(String.class))).thenReturn(null);

    // when
    runner.execute();

    // then
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest1"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest2"));
    verify(bucketWrapper, times(1)).insert(new ChangeEntry("Btest3"));
  }

}
