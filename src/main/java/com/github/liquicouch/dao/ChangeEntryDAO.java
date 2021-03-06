package com.github.liquicouch.dao;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.ParameterizedN1qlQuery;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.github.liquicouch.changeset.ChangeEntry;
import com.github.liquicouch.exception.LiquiCouchCounterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author deniswsrosa
 */
public class ChangeEntryDAO {
  private static final Logger logger = LoggerFactory.getLogger("LiquiCouch DAO");

  private BucketWrapper bucketWrapper;
  private Bucket bucket;

  public ChangeEntryDAO(Bucket bucket) {
    this.bucket = bucket;
    this.bucketWrapper = new BucketWrapper(bucket);
  }

  public void executeCount(ParameterizedN1qlQuery n1qlQuery, Bucket bucket, ChangeEntry changeEntry) {
    n1qlQuery.params().consistency(ScanConsistency.REQUEST_PLUS);

    for (int i = 0; i < (changeEntry.getRecounts()+1); i++) {

      N1qlQueryResult result = bucket.query(n1qlQuery);
      if (result.finalSuccess()) { //query executed entirely
        //tip: generally, only call allRows() once
        List<N1qlQueryRow> rows = result.allRows();

        if (rows.get(0).value().get("size") == null) {
          throw new LiquiCouchCounterException("There is no attribute called 'size' in the N1qlQuery returned by ChangeSet " + changeEntry.getChangeId() +
              ", class= " + changeEntry.getChangeLogClass() + ", method=" + changeEntry.getChangeSetMethodName());
        }

        long bucketSize = rows.get(0).value()
            .getLong("size");

        if (bucketSize == 0l) {
          return;
        } else {

          logger.warn("Counter have not returned size as 0 for changeSet " + changeEntry.getChangeId());
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      } else {
        throw new LiquiCouchCounterException("Could not execute the counter properly: " + result.errors());
      }
    }

    throw new LiquiCouchCounterException("All recount operations for changeSet " + changeEntry.getChangeId() + " have failed.");
  }

  public boolean isNewChange(ChangeEntry changeEntry) {
    JsonDocument entry = bucket.get(changeEntry._getId());
    return entry == null;
  }

  public void save(ChangeEntry changeEntry) {
    bucketWrapper.insert(changeEntry);
  }

  public void setBucketWrapper(BucketWrapper bucketWrapper){
    this.bucketWrapper = bucketWrapper;
  }

}
