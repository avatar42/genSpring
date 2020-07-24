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
		USAGE: Genspring [options] [table names]<br>
		Where options are:<br>
		-double = use Double instead of BigDecimal for entities beans<br>
		-toString = generate toString() methods for entities beans **(overridden by genSpring.beanToString in properties file)**<br>
		-beanEquals = generate equals() methods for entities beans<br>
		<br>
		if table names not given then runs on all tables in DB.<br>
		<br>
		Note: be sure to set properties in resources/genSpring.properties before running.<br>

# Output from generator
For example with a DB containing the tables Networks and Shows would generate the files <br>
#TODO: update files list
./pom.xml <br>
./src/main/java/com/dea42/watchlist/controller/AccountController.java
./src/main/java/com/dea42/watchlist/controller/ApiController.java
./src/main/java/com/dea42/watchlist/controller/AppController.java
./src/main/java/com/dea42/watchlist/controller/CustomErrorController.java
./src/main/java/com/dea42/watchlist/controller/NetworksController.java
./src/main/java/com/dea42/watchlist/controller/ShowsController.java
./src/main/java/com/dea42/watchlist/db/SQLiteDialect.java **(Only if using SQLite)**<br>
./src/main/java/com/dea42/watchlist/entity/Account.java
./src/main/java/com/dea42/watchlist/entity/Networks.java
./src/main/java/com/dea42/watchlist/entity/Shows.java
./src/main/java/com/dea42/watchlist/form/SignupForm.java
./src/main/java/com/dea42/watchlist/repo/AccountRepository.java
./src/main/java/com/dea42/watchlist/repo/NetworksRepository.java
./src/main/java/com/dea42/watchlist/repo/ShowsRepository.java
./src/main/java/com/dea42/watchlist/SecurityConfiguration.java
./src/main/java/com/dea42/watchlist/service/AccountService.java
./src/main/java/com/dea42/watchlist/service/NetworksServices.java
./src/main/java/com/dea42/watchlist/service/ShowsServices.java
./src/main/java/com/dea42/watchlist/ServletInitializer.java
./src/main/java/com/dea42/watchlist/utils/ExceptionHandler.java
./src/main/java/com/dea42/watchlist/utils/Message.java
./src/main/java/com/dea42/watchlist/utils/MessageHelper.java
./src/main/java/com/dea42/watchlist/utils/Utils.java
./src/main/java/com/dea42/watchlist/WebAppApplication.java
./src/main/resources/app.properties
./src/main/resources/application.properties
./src/main/resources/log4j2.xml
./src/main/resources/messages.properties
./src/main/resources/messages_de.properties
./src/main/resources/messages_fr.properties
./src/main/resources/resources/css/bootstrap.min.css
./src/main/resources/resources/css/site.css
./src/main/resources/resources/js/bootstrap.min.js
./src/main/resources/resources/js/jquery.min.js
./src/main/resources/templates/api_index.html
./src/main/resources/templates/edit_networks.html **(edit / create page)** <br>
./src/main/resources/templates/edit_shows.html **(edit / create page)** <br>
./src/main/resources/templates/error/general.html
./src/main/resources/templates/fragments/alert.html
./src/main/resources/templates/fragments/footer.html
./src/main/resources/templates/fragments/header.html **(nav header)** <br>
./src/main/resources/templates/home/homeNotSignedIn.html
./src/main/resources/templates/home/homeSignedIn.html
./src/main/resources/templates/home/signin.html
./src/main/resources/templates/home/signup.html
./src/main/resources/templates/index.html
./src/main/resources/templates/networkss.html **(list page)** <br>
./src/main/resources/templates/showss.html **(list page)** <br>
./src/main/webapp/favicon.ico ** sample site icon**<br>
./src/main/webapp/public/optView.html ** sample static web page**<br>
./src/main/webapp/public/Players.html ** sample static web page**<br>
./src/main/webapp/public/resources/sheet.css ** css file for Google sheet tabs exported as HTML**<br>
./src/main/webapp/resources/css/bootstrap.min.css
./src/main/webapp/resources/css/site.css
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.eot
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.svg
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.ttf
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.woff
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.woff2
./src/main/webapp/resources/js/bootstrap.min.js
./src/main/webapp/resources/js/jquery.min.js<br>
./src/main/webapp/WEB-INF/web.xml<br>
<br>
Test files<br>
./src/test/java/com/dea42/watchlist/controller/ApiControllerTest.java<br>
./src/test/java/com/dea42/watchlist/controller/AppControllerTest.java<br>
./src/test/java/com/dea42/watchlist/controller/NetworksControllerTest.java<br>
./src/test/java/com/dea42/watchlist/controller/ShowsControllerTest.java<br>
./src/test/java/com/dea42/watchlist/MockBase.java<br>
./src/test/java/com/dea42/watchlist/selenium/SeleniumBase.java ** base test class **<br>
./src/test/java/com/dea42/watchlist/selenium/SmokeIT.java ** integration tests against app server **<br>
./src/test/java/com/dea42/watchlist/selenium/SmokeTest.java ** integration tests against Spring Boot **<br>
./src/test/java/com/dea42/watchlist/UnitBase.java<br>
./src/test/java/com/dea42/watchlist/WebAppApplicationTest.java<br>
./src/test/java/com/dea42/watchlist/controller/ApiControllerTest.java <br>
./src/test/java/com/dea42/watchlist/controller/AppControllerTest.java <br>
./src/test/java/com/dea42/watchlist/controller/NetworksControllerTest.java <br>
./src/test/java/com/dea42/watchlist/controller/ShowsControllerTest.java <br>
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
