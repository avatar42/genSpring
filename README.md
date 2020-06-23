# genSpring
Create a simple Spring boot CRUD app with both web and REST interfaces from a DB complete with basic hard coded, in mem, auth system.
Tested with SQLite but code was brought over from Struts version for MySQL and SQLServer that will probably work.
Note for safety reasons it will not overwrite existing files so just delete the files you want to regen and run the tools again.

# Setup
Basically set your options in /genSpring/src/main/resources/genSpring.properties 

db.url=jdbc:sqlite:L:/SpringTools4.6.1/workspace/watchlistDB.sqlite <br>
db.driver=org.sqlite.JDBC <br>
## add these if needed
db.user= <br>
db.password= <br>
db.name= <br>
 
 
## top folder for output / new project root 
genSpring.outdir=../Watchlist 
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
Then run /genSpring/src/main/java/com/dea42/build/GenSpring.java as a console Java app. <br>
USAGE: GenSpring [-double] [-toString] [tableNames] <br>
-double = Use Double instead of BigDecimal <br>
-toString = Add toString() to bean classes **(overridden by genSpring.beanToString in properties file)** <br>
tableNames if given only generates files for those tables. If not given generates full project for all tables in DB **(not filtered see main())** <br>

# Output from generator
For example with a DB containing the tables Networks and Shows would generate the files <br>
#TODO: update files list
./pom.xml <br>
./src/main/java/com/dea42/watchlist/controller/ApiController.java <br>
./src/main/java/com/dea42/watchlist/controller/AppController.java <br>
./src/main/java/com/dea42/watchlist/controller/NetworksController.java <br>
./src/main/java/com/dea42/watchlist/controller/ShowsController.java <br>
./src/main/java/com/dea42/watchlist/db/SQLiteDialect.java **(Only if using SQLite)**<br>
./src/main/java/com/dea42/watchlist/entity/Networks.java <br>
./src/main/java/com/dea42/watchlist/entity/Shows.java <br>
./src/main/java/com/dea42/watchlist/repo/NetworksRepository.java <br>
./src/main/java/com/dea42/watchlist/repo/ShowsRepository.java <br>
./src/main/java/com/dea42/watchlist/SecurityConfiguration.java <br>
./src/main/java/com/dea42/watchlist/service/NetworksServices.java <br>
./src/main/java/com/dea42/watchlist/service/ShowsServices.java <br>
./src/main/java/com/dea42/watchlist/ServletInitializer.java <br>
./src/main/java/com/dea42/watchlist/WebAppApplication.java <br>
./src/main/resources/application.properties <br>
./src/main/resources/log4j2.xml <br>
./src/main/resources/resources/css/bootstrap.min.css <br>
./src/main/resources/resources/css/site.css <br>
./src/main/resources/resources/js/bootstrap.min.js <br>
./src/main/resources/resources/js/jquery.min.js <br>
./src/main/resources/templates/api_index.html <br>
./src/main/resources/templates/edit_networks.html **(edit / create page)** <br>
./src/main/resources/templates/edit_shows.html **(edit / create page)** <br>
./src/main/resources/templates/index.html **(links to list pages plus /api and /login)** <br>
./src/main/resources/templates/login.html ** login page**<br>
./src/main/resources/templates/networkss.html **(list page)** <br>
./src/main/resources/templates/showss.html **(list page)** <br>
./src/main/webapp/favicon.ico ** sample site icon**<br>
./src/main/webapp/optView.html ** sample static web page**<br>
./src/main/webapp/Players.html ** sample static web page**<br>
./src/main/webapp/resources/sheet.css ** css file for Google sheet tabs exported as HTML**<br>
./src/test/java/com/dea42/watchlist/controller/ApiControllerTest.java <br>
./src/test/java/com/dea42/watchlist/controller/AppControllerTest.java <br>
./src/test/java/com/dea42/watchlist/controller/NetworksControllerTest.java <br>
./src/test/java/com/dea42/watchlist/controller/ShowsControllerTest.java <br>
./src/test/java/com/dea42/watchlist/selenium/SeleniumBase.java ** base test class **<br>
./src/test/java/com/dea42/watchlist/selenium/SmokeIT.java ** integration tests against app server **<br>
./src/test/java/com/dea42/watchlist/selenium/SmokeTest.java ** integration tests against Spring Boot **<br>
./src/test/java/com/dea42/watchlist/WebAppApplicationTest.java <br>
 <br>
and the folder <br>
./src/test/resources <br>
 
 Use the integration maven profile to run test of a cargo deployed version of the app
 
# Running the web app
- Import generated project as existing maven project
- Do maven install ** Note this runs a basic test of the build as well **
- Run /gen/src/main/java/com/dea42/watchlist/WebAppApplication.java as Java or Spring Boot app
- Wait for start up
- Point browser to  http://localhost:8080/

# Sheet2DB
Creates a DB from a Google sheet with a table for each selected tab <br>
 
# Setup
Basically set your options in /genSpring/src/main/resources/sheet.properties <br>
 
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
shows.columns=A-I,Q-T,BC-BF <br>
Roamio_Todo.columns=D-G,M-O <br>
Roamio_sp.columns=A-J <br>
Roamio_npl.columns=A-K <br>

### required (not null) columns. Columns with null in the field will not be imported.
Networks.required=A <br>
shows.required=A <br>
Roamio_npl.required=A <br>
Roamio_sp.required=A <br>
Roamio_Todo.required=A <br>
OTA.required=H <br>
CableCard.required=A,B <br>

### set this if you need to ignore extra rows at bottom of sheet
shows.lastRow=251 <br>
