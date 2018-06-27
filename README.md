![LiquiCouch](https://raw.githubusercontent.com/deniswsrosa/liquicouch/master/misc/banner.png)

[![Build Status](https://travis-ci.org/LiquiCouch/LiquiCouch.svg?branch=master)](https://travis-ci.org/LiquiCouch/LiquiCouch) [![Coverity Scan Build Status](https://scan.coverity.com/projects/2721/badge.svg)](https://scan.coverity.com/projects/2721) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.liquicouch/LiquiCouch/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.liquicouch/LiquiCouch) [![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/deniswsrosa/liquicouch/blob/master/LICENSE)
---


**LiquiCouch** is a Java framework which helps you to *manage changes* in your Couchbase and *synchronize* them with your application.
The concept is very similar to other db migration tools such as [Liquibase](http://www.liquibase.org) or [Flyway](http://flywaydb.org) but *without using XML/JSON/YML files*.

The goal is to keep this tool simple and comfortable to use.


**LiquiCouch** provides new approach for adding changes (change sets) based on Java classes and methods with appropriate annotations.

# Getting started

## Add a dependency

*IMPORTANT:* I'm still waiting for maven central to publish the artifact, should be ready in less then 3 days.

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

Method annotated by @ChangeSet is taken and applied to the database. History of applied change sets is stored in a document with type `dbChangeLog`

#### Annotation parameters:

`order` - string for sorting change sets in one changelog. Sorting in alphabetical order, ascending. It can be a number, a date etc.

`id` - name of a change set, **must be unique** for all change logs in a database

`author` - author of a change set

`runAlways` - _[optional, default: false]_ changeset will always be executed but only first execution event will be stored as a document

`recounts` - _[optional, default: 0] [Only applied when changSet returns a ParameterizedN1qlQuery]_ if you want to be sure that all documents have been update, you can return a ParameterizedN1qlQuery. This query expects a result called *size*. If size is not zero, the query will be executed again according to the number of recounts specified. If none of the recounts returns zero, an exception will be thrown, and the application will fail to start.

`retries` - _[optional, default: 0] [Only applied when changSet returns a ParameterizedN1qlQuery]_ if the recount operation fails (the count result isn't zero) it will rerun the changeSet in an attempt to update the remaining documents ( Your changeSet should me able to run multiple times without any side effects). If all retries fail, an exception will the thrown an the application will fail to start.

#### Defining ChangeSet methods
Method annotated by `@ChangeSet` can have one of the following definition:


##### With Spring

```java

/**
 * If you are using Spring, you can Autowire your Services or Repositories
 */
@Component
@ChangeLog(order = "001")
public class Migration1 {

    @Autowired // Yes, You can a
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @ChangeSet(order = "001", id = "someChangeId1", author = "testAuthor")
    public void importantWorkToDo(Bucket bucket){
        System.out.println("----------Migration1 - Method1");
    }

    @ChangeSet(order = "002", id = "someChangeId2", author = "testAuthor")
    public void method2(Bucket bucket){
        System.out.println("----------Migration1 - Method2");
    }

    @ChangeSet(order = "003", id = "someChangeId3", author = "testAuthor")
    public void method3(Bucket bucket){
        System.out.println("----------Migration1 - Method3");
    }

    @ChangeSet(order = "004", id = "someChangeId4", author = "testAuthor")
    public void method4(Bucket bucket){
        System.out.println("----------Migration1 - Method4");
    }

    @ChangeSet(order = "005", id = "someChangeId5", author = "testAuthor")
    public void method5(){
        System.out.println("----------Migration1 - Method5 (The bucket parameter is not necessary here)");
    }


    /**
     * Here is an example of how you can check if your update has run successfully, all you need to do is to
     * return a ParameterizedN1qlQuery.
     * @return
     */
    @ChangeSet(order = "006", id = "someChangeId6", author = "testAuthor", recounts = "2", retries = "1")
    public ParameterizedN1qlQuery method6(){

        //adding some data as an example
        userService.save(new User("someUserIdForTesting", "user1", new Address(), new ArrayList<>(), Arrays.asList("admin", "manager")));
        Iterable<User> users = userRepository.findAll(new PageRequest(0, 100));//we just care about the first 100 records

        users.forEach( e-> {
            //rename admin to adm
             if(e.getSecurityRoles().contains("admin")) {
                 e.getSecurityRoles().remove("admin");
                 e.getSecurityRoles().add("adm");
             }
            userRepository.save(e);
        });

        //IMPORTANT: The query MUST have an attribute called *size*
        String queryString = "Select count(userRole)  as size from test t unnest t.securityRoles as userRole " +
                " where t._class='com.cb.springdata.sample.entities.User' " +
                " and userRole = 'admin'";
        N1qlParams params = N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS).adhoc(true);
        ParameterizedN1qlQuery query = N1qlQuery.parameterized(queryString, JsonObject.create(), params);
        return query;
    }

}


```


##### Without Spring

```java
/**
 * This is an example of how to use it without Spring, in this case you can execute all the queries via the Bucket argument.
 */
@ChangeLog(order = "2")
public class Migration2 {

    @ChangeSet(order = "1", id = "someChangeId21", author = "testAuthor")
    public void importantWorkToDo(){
        System.out.println("----------Migration2 - Method1");
    }

    @ChangeSet(order = "2", id = "someChangeId22", author = "testAuthor")
    public void method2(){
        System.out.println("----------Migration2 - Method2");
    }

    @ChangeSet(order = "3", id = "someChangeId23", author = "testAuthor")
    public void method3(){
        System.out.println("----------Migration2 - Method3");
    }

    @ChangeSet(order = "4", id = "someChangeId24", author = "testAuthor")
    public void method4(){
        System.out.println("----------Migration2 - Method4");
    }

    @ChangeSet(order = "5", id = "someChangeId25", author = "testAuthor")
    public void method5(Bucket bucket){
        System.out.println("----------Migration2 - Method5");
    }

    @ChangeSet(order = "6", id = "someChangeId256", author = "testAuthor", runAlways=true)
    public void method6(Bucket bucket){
        System.out.println("----------Migration2 - Method6 - THIS SHOULD ALWAYS RUN "+bucket.name());
    }
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

### Enabling @Profile annotation (optional)
      
To enable the `@Profile` integration, please inject `org.springframework.context.ApplicationContext` to you runner.

```java

@Autowired
private ApplicationContext context;

@Bean
public Liquicouch LiquiCouch() {
  Liquicouch runner = new Liquicouch(context);
  //... etc
}
```

## Support

If you have any questions/requests, just ping me on twitter at @deniswsrosa

## Special Thanks

This project is a fork of MongoBee, so thanks to all guys involved with it.