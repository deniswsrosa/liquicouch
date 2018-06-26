package com.github.liquicouch.utils;

import com.github.liquicouch.changeset.ChangeEntry;
import com.github.liquicouch.exception.LiquiCouchChangeSetException;
import com.github.liquicouch.test.changelogs.*;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author deniswsrosa
 */
public class ChangeServiceTest {

  @Test
  public void shouldFindChangeLogClasses(){
    // given
    String scanPackage = LiquiCouchTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    // when
    List<Class<?>> foundClasses = service.fetchChangeLogs();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }
  
  @Test
  public void shouldFindChangeSetMethods() throws LiquiCouchChangeSetException {
    // given
    String scanPackage = LiquiCouchTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(LiquiCouchTestResource.class);
    
    // then
    assertTrue(foundMethods != null && foundMethods.size() == 3);
  }

  @Test
  public void shouldFindAnotherChangeSetMethods() throws LiquiCouchChangeSetException {
    // given
    String scanPackage = LiquiCouchTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(LiquiCouchChange2TestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 3);
  }


  @Test
  public void shouldFindIsRunAlwaysMethod() throws LiquiCouchChangeSetException {
    // given
    String scanPackage = LiquiCouchTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(LiquiCouchChange2TestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangeSetWithAlways")){
        assertTrue(service.isRunAlwaysChangeSet(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeSet(foundMethod));
      }
    }
  }

  @Test
  public void shouldCreateEntry() throws LiquiCouchChangeSetException {
    
    // given
    String scanPackage = LiquiCouchTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    List<Method> foundMethods = service.fetchChangeSets(LiquiCouchTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = service.createChangeEntry(foundMethod);
      
      // then
      Assert.assertEquals("testuser", entry.getAuthor());
      Assert.assertEquals(LiquiCouchTestResource.class.getName(), entry.getChangeLogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangeSetMethodName());
    }
  }

  @Test(expected = LiquiCouchChangeSetException.class)
  public void shouldFailOnDuplicatedChangeSets() throws LiquiCouchChangeSetException {
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    service.fetchChangeSets(ChangeLogWithDuplicate.class);
  }

}
