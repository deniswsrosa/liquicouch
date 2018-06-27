package com.github.liquicouch;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.query.ParameterizedN1qlQuery;
import com.github.liquicouch.changeset.ChangeEntry;
import com.github.liquicouch.dao.ChangeEntryDAO;
import com.github.liquicouch.exception.LiquiCouchChangeSetException;
import com.github.liquicouch.exception.LiquiCouchConfigurationException;
import com.github.liquicouch.exception.LiquiCouchCounterException;
import com.github.liquicouch.exception.LiquiCouchException;
import com.github.liquicouch.utils.ChangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * LiquiCouch runner
 *
 * @author deniswsrosa
 */
public class LiquiCouch implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(LiquiCouch.class);

  private ChangeEntryDAO dao;

  private boolean enabled = true;
  private String changeLogsScanPackage;
  private Bucket bucket;
  private ApplicationContext context;
  private Environment springEnvironment;


  public LiquiCouch(ApplicationContext context) {
    this.context = context;
    this.springEnvironment = context.getEnvironment();
    List<String> nodes = Arrays.asList(springEnvironment.getProperty("spring.couchbase.bootstrap-hosts").split(","));
      this.bucket = CouchbaseCluster.create(nodes)
          .openBucket(springEnvironment.getProperty("spring.couchbase.bucket.name"),
              springEnvironment.getProperty("spring.couchbase.bucket.password"));
      this.dao = new ChangeEntryDAO(bucket);
  }


  public LiquiCouch(Bucket bucket) {
    this.bucket = bucket;
    this.dao = new ChangeEntryDAO(bucket);
  }

  public LiquiCouch(List<String> nodes, String bucketName, String password) {
    this(CouchbaseCluster.create(nodes).openBucket(bucketName, password));
  }

  public LiquiCouch(String connectionString, String bucketName, String password) {
    this(CouchbaseCluster.fromConnectionString(connectionString).openBucket(bucketName, password));
  }

  /**
   * For Spring users: executing liquicouch after bean is created in the Spring context
   *
   * @throws Exception exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    execute();
  }

  /**
   * Executing migration
   *
   * @throws LiquiCouchException exception
   */
  public void execute() throws LiquiCouchException {
    if (!isEnabled()) {
      logger.info("LiquiCouch is disabled. Exiting.");
      return;
    }

    validateConfig();

    logger.info("LiquiCouch is starting the data migration sequence..");

    try {
      executeMigration();
    } finally {
      logger.info("The buckes is being closed.");
      bucket.close();
    }

    logger.info("LiquiCouch has finished his job.");
  }

  private void executeMigration() throws LiquiCouchException {

    ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

    for (Class<?> changelogClass : service.fetchChangeLogs()) {

      Object changelogInstance = null;
      try {
        changelogInstance = changelogClass.getConstructor().newInstance();

        if (context != null) {
          context.getAutowireCapableBeanFactory().autowireBean(changelogInstance);
        }

        List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

        for (Method changesetMethod : changesetMethods) {
          ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

          try {
            if (dao.isNewChange(changeEntry)) {
              executeMethod(changesetMethod, changelogInstance, changeEntry);
              dao.save(changeEntry);
              logger.info(changeEntry + " applied");

            } else if (service.isRunAlwaysChangeSet(changesetMethod)) {
              executeMethod(changesetMethod, changelogInstance, changeEntry);
              logger.info(changeEntry + " reapplied");

            } else {
              logger.info(changeEntry + " passed over");
            }
          } catch (LiquiCouchChangeSetException e) {
            logger.error(e.getMessage());
          }
        }
      } catch (NoSuchMethodException e) {
        throw new LiquiCouchException(e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new LiquiCouchException(e.getMessage(), e);
      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new LiquiCouchException(targetException.getMessage(), e);
      } catch (InstantiationException e) {
        throw new LiquiCouchException(e.getMessage(), e);
      }
    }
  }

  private void executeMethod(Method changesetMethod, Object changelogInstance, ChangeEntry entry) throws LiquiCouchException {
    for (int i = 0; i < (entry.getRetries()+1); i++) {
      try {
        Object object = executeChangeSetMethod(changesetMethod, changelogInstance, bucket);

        if(object == null || !(object instanceof ParameterizedN1qlQuery) ) {
          return;
        } else {
          dao.executeCount((ParameterizedN1qlQuery) object, bucket, entry);
        }

      } catch(LiquiCouchCounterException e) {
        //if is the last retry throw the exception
        if (i == entry.getRetries()) {
          throw new LiquiCouchException("All retries have failed for changeSet " + entry.getChangeId(), e);
        } else {
          logger.warn("All recounts have failed, retrying to execute changeSet " + entry.getChangeId());
        }

      } catch (IllegalAccessException e) {
        throw new LiquiCouchException(e.getMessage(), e);

      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new LiquiCouchException(targetException.getMessage(), e);
      }
    }
  }

  private Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance, Bucket bucket)
      throws IllegalAccessException, InvocationTargetException, LiquiCouchChangeSetException {
    if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(Bucket.class)) {
      logger.debug("method with bucket argument");

      return changeSetMethod.invoke(changeLogInstance, bucket);
    } else if (changeSetMethod.getParameterTypes().length == 0) {
      logger.debug("method with no params");
      return changeSetMethod.invoke(changeLogInstance);
    } else {
      throw new LiquiCouchChangeSetException("ChangeSet method " + changeSetMethod.getName() +
          " has wrong arguments list. Please see docs for more info!");
    }
  }

  private void validateConfig() throws LiquiCouchConfigurationException {
    if (!hasText(changeLogsScanPackage)) {
      throw new LiquiCouchConfigurationException("Scan package for changelogs is not set: use appropriate setter");
    }
  }


  /**
   * Package name where @ChangeLog-annotated classes are kept.
   *
   * @param changeLogsScanPackage package where your changelogs are
   * @return LiquiCouch object for fluent interface
   */
  public LiquiCouch setChangeLogsScanPackage(String changeLogsScanPackage) {
    this.changeLogsScanPackage = changeLogsScanPackage;
    return this;
  }

  /**
   * @return true if LiquiCouch runner is enabled and able to run, otherwise false
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Feature which enables/disables LiquiCouch runner execution
   *
   * @param enabled LiquiCouch will run only if this option is set to true
   * @return LiquiCouch object for fluent interface
   */
  public LiquiCouch setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Set Environment object for Spring Profiles (@Profile) integration
   *
   * @param environment org.springframework.core.env.Environment object to inject
   * @return LiquiCouch object for fluent interface
   */
  public LiquiCouch setSpringEnvironment(Environment environment) {
    this.springEnvironment = environment;
    return this;
  }

  /**
   * Should only be used for testing purposes
   */
  public void setDAO(ChangeEntryDAO changeEntryDAO){
    this.dao = changeEntryDAO;
  }


  public static void main(String[] args) {
    CouchbaseCluster.create("localhost")
        .openBucket("test","couchbase");
  }
}
