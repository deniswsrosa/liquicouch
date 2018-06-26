![LiquiCouch](https://raw.githubusercontent.com/deniswsrosa/liquicouch/master/misc/banner.png)

[![Build Status](https://travis-ci.org/LiquiCouch/LiquiCouch.svg?branch=master)](https://travis-ci.org/LiquiCouch/LiquiCouch) [![Coverity Scan Build Status](https://scan.coverity.com/projects/2721/badge.svg)](https://scan.coverity.com/projects/2721) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.liquicouch/LiquiCouch/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.liquicouch/LiquiCouch) [![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/deniswsrosa/liquicouch/blob/master/LICENSE)
---


**LiquiCouch** is a Java tool which helps you to *manage changes* in your Couchbase and *synchronize* them with your application.
The concept is very similar to other db migration tools such as [Liquibase](http://www.liquibase.org) or [Flyway](http://flywaydb.org) but *without using XML/JSON/YML files*.

The goal is to keep this tool simple and comfortable to use.


**LiquiCouch** provides new approach for adding changes (change sets) based on Java classes and methods with appropriate annotations.

# Getting started

## Add a dependency

With Maven
```xml
<dependency>
  <groupId>com.github.liquicouch</groupId>
  <artifactId>liquicouch</artifactId>
  <version>0.1</version>
</dependency>
```
With Gradle
```groovy
compile 'org.javassist:javassist:3.18.2-GA' // workaround for ${javassist.version} placeholder issue*
compile 'com.github.liquicouch:liquicouch:0.13'
```

## Usage with Spring

You need to instantiate Liquicouch object and provide some configuration.
If you use Spring can be instantiated as a singleton bean in the Spring context. 
In this case the migration process will be executed automatically on startup.

```java

@Autowired
private Environment environment;

@Bean
public LiquiCouch liquicouch(){
  LiquiCouch runner = new LiquiCouch(environment); //It will grab all the data needed from the application.properties file
  runner.setChangeLogsScanPackage(
       "com.example.yourapp.changelogs"); // the package to be scanned for changesets
  
  return runner;
}
```


## Usage without Spring
Using LiquiCouch without a spring context has similar configuration but you have to remember to run `execute()` method to start a migration process.

```java
LiquiCouch runner = new LiquiCouch("couchbase://SOME_IP_ADDRESS", "yourBucketName", "bucketPasword");
runner.setChangeLogsScanPackage(
     "com.example.yourapp.changelogs"); // package to scan for changesets

runner.execute();         //  ------> starts migration changesets
```

Above examples provide minimal configuration. `Liquicouch` object provides some other possibilities (setters) to make the tool more flexible:

```java
runner.setEnabled(shouldBeEnabled);              // default is true, migration won't start if set to false
```


## Creating change logs

`ChangeLog` contains bunch of `ChangeSet`s. `ChangeSet` is a single task (set of instructions made on a database). In other words `ChangeLog` is a class annotated with `@ChangeLog` and containing methods annotated with `@ChangeSet`.

```java 
package com.example.yourapp.changelogs;

@ChangeLog
public class DatabaseChangelog {
  
  @ChangeSet(order = "1", id = "someChangeId", author = "testAuthor")
  public void importantWorkToDo(){
     // task implementation
  }

}
```
### @ChangeLog

Class with change sets must be annotated by `@ChangeLog`. There can be more than one change log class but in that case `order` argument should be provided:

```java
@ChangeLog(order = "001")
public class DatabaseChangelog {
  //...
}
```
ChangeLogs are sorted *alphabetically* (that is why it is a good practice to start the order with zeros) by `order` argument and changesets are applied due to this order.

### @ChangeSet

Method annotated by @ChangeSet is taken and applied to the database. History of applied change sets is stored in a document with type `dbchangelog`

#### Annotation parameters:

`order` - string for sorting change sets in one changelog. Sorting in alphabetical order, ascending. It can be a number, a date etc.

`id` - name of a change set, **must be unique** for all change logs in a database

`author` - author of a change set

`runAlways` - _[optional, default: false]_ changeset will always be executed but only first execution event will be stored in dbchangelog collection

#### Defining ChangeSet methods
Method annotated by `@ChangeSet` can have one of the following definition:

```java
@ChangeSet(order = "001", id = "someChangeWithoutArgs", author = "testAuthor")
public void someChange1() {
   // method without arguments can do some non-db changes
}

@ChangeSet(order = "002", id = "someChangeWithMongoDatabase", author = "testAuthor")
public void someChange2(MongoDatabase db) {
  // type: com.mongodb.client.MongoDatabase : original MongoDB driver v. 3.x, operations allowed by driver are possible
  // example: 
  MongoCollection<Document> mycollection = db.getCollection("mycollection");
  Document doc = new Document("testName", "example").append("test", "1");
  mycollection.insertOne(doc);
}

@ChangeSet(order = "003", id = "someChangeWithDb", author = "testAuthor")
public void someChange3(DB db) {
  // This is deprecated in mongo-java-driver 3.x, use MongoDatabase instead
  // type: com.mongodb.DB : original MongoDB driver v. 2.x, operations allowed by driver are possible
  // example: 
  DBCollection mycollection = db.getCollection("mycollection");
  BasicDBObject doc = new BasicDBObject().append("test", "1");
  mycollection .insert(doc);
}

@ChangeSet(order = "004", id = "someChangeWithJongo", author = "testAuthor")
public void someChange4(Jongo jongo) {
  // type: org.jongo.Jongo : Jongo driver can be used, used for simpler notation
  // example:
  MongoCollection mycollection = jongo.getCollection("mycollection");
  mycollection.insert("{test : 1}");
}

@ChangeSet(order = "005", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange5(MongoTemplate mongoTemplate) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // Spring Data integration allows using MongoTemplate in the ChangeSet
  // example:
  mongoTemplate.save(myEntity);
}

@ChangeSet(order = "006", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange5(MongoTemplate mongoTemplate, Environment environment) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // type: org.springframework.core.env.Environment
  // Spring Data integration allows using MongoTemplate and Environment in the ChangeSet
}
```

## Using Spring profiles
     
**LiquiCouch** accepts Spring's `org.springframework.context.annotation.Profile` annotation. If a change log or change set class is annotated  with `@Profile`, 
then it is activated for current application profiles.

_Example 1_: annotated change set will be invoked for a `dev` profile
```java
@Profile("dev")
@ChangeSet(author = "testuser", id = "myDevChangest", order = "01")
public void devEnvOnly(DB db){
  // ...
}
```
_Example 2_: all change sets in a changelog will be invoked for a `test` profile
```java
@ChangeLog(order = "1")
@Profile("test")
public class ChangelogForTestEnv{
  @ChangeSet(author = "testuser", id = "myTestChangest", order = "01")
  public void testingEnvOnly(DB db){
    // ...
  } 
}
```

### Enabling @Profile annotation (option)
      
To enable the `@Profile` integration, please inject `org.springframework.core.env.Environment` to you runner.

```java      
@Bean @Autowired
public Liquicouch LiquiCouch(Environment environment) {
  Liquicouch runner = new Liquicouch(uri);
  runner.setSpringEnvironment(environment)
  //... etc
}
```
