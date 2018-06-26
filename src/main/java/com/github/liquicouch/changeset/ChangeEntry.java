package com.github.liquicouch.changeset;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import java.util.Date;
import java.util.Objects;


public class ChangeEntry {
  private static final String KEY_CHANGEID = "changeId";
  private static final String KEY_AUTHOR = "author";
  private static final String KEY_TIMESTAMP = "timestamp";
  private static final String KEY_RECOUNTS = "recounts";
  private static final String KEY_CHANGELOGCLASS = "changeLogClass";
  private static final String KEY_CHANGESETMETHOD = "changeSetMethod";

  private String _id;
  private String changeId;
  private String author;
  private Date timestamp;
  private Integer recounts;
  private String changeLogClass;
  private String changeSetMethodName;
  
  public ChangeEntry(String changeId, String author, Date timestamp, Integer recounts, String changeLogClass, String changeSetMethodName) {
    this.changeId = changeId;
    this._id = "ChangeEntry::"+changeId;
    this.author = author;
    this.timestamp = new Date(timestamp.getTime());
    this.recounts = recounts;
    this.changeLogClass = changeLogClass;
    this.changeSetMethodName = changeSetMethodName;
  }

  public ChangeEntry(String changeId) {
    this.changeId = changeId;
    this._id = "ChangeEntry::"+changeId;
  }

  @Override
  public String toString() {
    return "[ChangeSet: id=" + this.changeId +
        ", author=" + this.author +
        ", changeLogClass=" + this.changeLogClass +
        ", changeSetMethod=" + this.changeSetMethodName + "]";
  }

  public String _getId() {
    return this._id;
  }

  public String getChangeId() {
    return this.changeId;
  }

  public String getAuthor() {
    return this.author;
  }

  public Date getTimestamp() {
    return this.timestamp;
  }

  public String getChangeLogClass() {
    return this.changeLogClass;
  }

  public String getChangeSetMethodName() {
    return this.changeSetMethodName;
  }

  public Integer getRecounts(){return this.recounts;}


  public static JsonDocument getAsJsonDocument(ChangeEntry changeEntry){
    JsonObject data = JsonObject.create()
        .put("type", "dbChangeLog")
        .put("id", changeEntry._getId())
        .put(ChangeEntry.KEY_CHANGEID, changeEntry.getChangeId())
        .put(ChangeEntry.KEY_AUTHOR, changeEntry.getAuthor())
        .put(ChangeEntry.KEY_CHANGELOGCLASS, changeEntry.getChangeLogClass())
        .put(ChangeEntry.KEY_CHANGESETMETHOD, changeEntry.getChangeSetMethodName())
        .put(ChangeEntry.KEY_RECOUNTS, changeEntry.getRecounts())
        .put(ChangeEntry.KEY_TIMESTAMP, changeEntry.getTimestamp() == null? null: changeEntry.getTimestamp().getTime());

    return JsonDocument.create(changeEntry._getId(), data);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChangeEntry that = (ChangeEntry) o;
    return Objects.equals(_id, that._id) &&
        Objects.equals(changeId, that.changeId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(_id, changeId);
  }
}
