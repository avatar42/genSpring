# genSpring
Create a simple Spring boot CRUD app with both web and REST interfaces from a DB complete with basic hard coded, in mem, auth system.
Tested with SQLite but code was brought over from Struts version for MySQL and SQLServer that will probably work.
Note for safety reasons it will overwrite existing files so just delete the files you want to regen and run the tools again.

# Setup
Basically set your options in /genSpring/src/main/resources/genSpring.properties

db.url=jdbc:sqlite:L:/SpringTools4.6.1/workspace/watchlistDB.sqlite
db.driver=org.sqlite.JDBC
## add these if needed
db.user=
db.password=
db.name=


## top folder for output / new project root 
genSpring.outdir=../gen
## used if not empty. Current year added to end.
genSpring.Copyright=Copyright (c) 2001-
## ignored if blank
genSpring.Company=RMRR
## defaults to 1.0
genSpring.version=1.0
## base package / groupId
genSpring.pkg=com.dea42
## sub package / artifactId
genSpring.module=watchlist
## add a toString() to the entity bean class
genSpring.beanToString=true


# Running the generator
Then run /genSpring/src/main/java/com/dea42/build/GenSpring.java as a console Java app.
USAGE: GenSpring [-double] [-toString] [tableNames]
-double = Use Double instead of BigDecimal
-toString = Add toString() to bean classes **(overridden by genSpring.beanToString in properties file)**
tableNames if given only generates files for those tables. If not given generates full project for all tables in DB **(not filtered see main())**

# Output from generator
For example with a DB containing the tables Networks and Shows would generate the files
./pom.xml
./src/main/java/com/dea42/watchlist/controller/ApiController.java
./src/main/java/com/dea42/watchlist/controller/AppController.java
./src/main/java/com/dea42/watchlist/controller/NetworksController.java
./src/main/java/com/dea42/watchlist/controller/ShowsController.java
./src/main/java/com/dea42/watchlist/db/SQLiteDialect.java
./src/main/java/com/dea42/watchlist/entity/Networks.java
./src/main/java/com/dea42/watchlist/entity/Shows.java
./src/main/java/com/dea42/watchlist/repo/NetworksRepository.java
./src/main/java/com/dea42/watchlist/repo/ShowsRepository.java
./src/main/java/com/dea42/watchlist/SecurityConfiguration.java
./src/main/java/com/dea42/watchlist/service/NetworksServices.java
./src/main/java/com/dea42/watchlist/service/ShowsServices.java
./src/main/java/com/dea42/watchlist/WebAppApplication.java
./src/main/resources/application.properties
./src/main/resources/static/css/site.css
./src/main/resources/templates/edit_networks.html **(edit / create page)**
./src/main/resources/templates/edit_shows.html
./src/main/resources/templates/index.html **(links to list pages plus /api and /login)**
./src/main/resources/templates/login.html
./src/main/resources/templates/networkss.html **(list page)**
./src/main/resources/templates/showss.html
./src/test/java/com/dea42/watchlist/WebAppApplicationTest.java

and the folder
./src/test/resources

# Running the web app
- Import generated project as existing maven project
- Do maven install ** Note this runs a basic test of the build as well **
- Run /gen/src/main/java/com/dea42/watchlist/WebAppApplication.java as Java or Spring Boot app
- Wait for start up
- Point browser to  http://localhost:8080/

# Sheet2DB
Creates a DB from a Google sheet with a table for each selected tab

# Setup
Basically set your options in /genSpring/src/main/resources/sheet.properties

## DB to create tables in
db.url=jdbc:sqlite:L:/sites/git/genSpring/target/watchlistDB.sqlite
## DB driver to use
db.driver=org.sqlite.JDBC

## Google sheet to read from
sheet.id=1-xYv1AVkUC5J3Tqpy2_3alZ5ZpBPnnO2vUGUCUeLVVE
## tabs to export
sheet.tabs=Networks,shows,Roamio_npl,Roamio_sp,Roamio_Todo,OTA,CableCard

## tab adjustments
### columns to export. If missing exports all existing
shows.columns=A-I,Q-T,BC-BF
Roamio_Todo.columns=D-G,M-O
Roamio_sp.columns=A-J
Roamio_npl.columns=A-K

### required (not null) columns. Columns with null in the field will not be imported.
Networks.required=A
shows.required=A
Roamio_npl.required=A
Roamio_sp.required=A
Roamio_Todo.required=A
OTA.required=H
CableCard.required=A,B

### set this if you need to ignore extra rows at bottom of sheet
shows.lastRow=251
