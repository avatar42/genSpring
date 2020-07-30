# Sheet2DB
Creates a DB from a Google sheet with a table for each selected tab <br>
 
# Setup
Basically set your options in /genSpring/src/main/resources/sheet.properties <br>
** Note after a clean you will be asked to auth to Google to access the sheet. See target/tokens folder** 
 
## DB to create tables in (note if blank and db.driver=org.sqlite.JDBC creates a SQLite DB in sheet.outdir folder. See Db.getUrl(ResourceBundle bundle, String folder))
db.url=jdbc:sqlite:L:/sites/git/genSpring/target/watchlistDB.sqlite 
## DB driver to use
db.driver=org.sqlite.JDBC 
## optional output location
sheet.outdir=../genSpringTest
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
genSpring.outdir=../genSpringTest 
## used if not empty. Current year added to end.
genSpring.Copyright=Copyright (c) 2001- 
## ignored if blank
genSpring.Company=RMRR 
## defaults to 1.0
genSpring.version=1.0 
## base package / groupId
genSpring.pkg=com.dea42 
## sub package / artifactId
genSpring.module=genSpring 
## Optional if the artifactId needs to be diff from module. Defaults to same as module.
genSpring.artifactId=genSpringTest
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
For example with a DB containing the tables Sheet1 and Sheet2 would generate the files <br>
#TODO: update files list
./pom.xml <br>
./src/main/java/com/dea42/genSpring/controller/AccountController.java<br>
./src/main/java/com/dea42/genSpring/controller/ApiController.java<br>
./src/main/java/com/dea42/genSpring/controller/AppController.java<br>
./src/main/java/com/dea42/genSpring/controller/CustomErrorController.java<br>
./src/main/java/com/dea42/genSpring/controller/Sheet1Controller.java<br>
./src/main/java/com/dea42/genSpring/controller/Sheet2Controller.java<br>
./src/main/java/com/dea42/genSpring/db/SQLiteDialect.java<br>
./src/main/java/com/dea42/genSpring/entity/Account.java<br>
./src/main/java/com/dea42/genSpring/entity/Sheet1.java<br>
./src/main/java/com/dea42/genSpring/entity/Sheet2.java<br>
./src/main/java/com/dea42/genSpring/form/SignupForm.java<br>
./src/main/java/com/dea42/genSpring/repo/AccountRepository.java<br>
./src/main/java/com/dea42/genSpring/repo/Sheet1Repository.java<br>
./src/main/java/com/dea42/genSpring/repo/Sheet2Repository.java<br>
./src/main/java/com/dea42/genSpring/SecurityConfiguration.java<br>
./src/main/java/com/dea42/genSpring/service/AccountService.java<br>
./src/main/java/com/dea42/genSpring/service/Sheet1Services.java<br>
./src/main/java/com/dea42/genSpring/service/Sheet2Services.java<br>
./src/main/java/com/dea42/genSpring/ServletInitializer.java<br>
./src/main/java/com/dea42/genSpring/utils/ExceptionHandler.java<br>
./src/main/java/com/dea42/genSpring/utils/Message.java<br>
./src/main/java/com/dea42/genSpring/utils/MessageHelper.java<br>
./src/main/java/com/dea42/genSpring/utils/Utils.java<br>
./src/main/java/com/dea42/genSpring/WebAppApplication.java<br>
./src/main/resources/app.properties<br>
./src/main/resources/application.properties<br>
./src/main/resources/log4j2.xml<br>
./src/main/resources/messages.properties<br>
./src/main/resources/messages_de.properties<br>
./src/main/resources/messages_fr.properties<br>
./src/main/resources/resources/css/bootstrap.min.css<br>
./src/main/resources/resources/css/site.css<br>
./src/main/resources/resources/js/bootstrap.min.js<br>
./src/main/resources/resources/js/jquery.min.js<br>
./src/main/resources/templates/api_index.html<br>
./src/main/resources/templates/edit_sheet1.html<br>
./src/main/resources/templates/edit_sheet2.html<br>
./src/main/resources/templates/error/general.html<br>
./src/main/resources/templates/fragments/alert.html<br>
./src/main/resources/templates/fragments/footer.html<br>
./src/main/resources/templates/fragments/header.html<br>
./src/main/resources/templates/home/homeNotSignedIn.html<br>
./src/main/resources/templates/home/homeSignedIn.html<br>
./src/main/resources/templates/home/signin.html<br>
./src/main/resources/templates/home/signup.html<br>
./src/main/resources/templates/index.html<br>
./src/main/resources/templates/sheet1s.html<br>
./src/main/resources/templates/sheet2s.html<br>
./src/main/webapp/favicon.ico<br>
./src/main/webapp/public/optView.html<br>
./src/main/webapp/public/Players.html<br>
./src/main/webapp/public/resources/sheet.css<br>
./src/main/webapp/resources/css/bootstrap.min.css<br>
./src/main/webapp/resources/css/site.css<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.eot<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.svg<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.ttf<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.woff<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.woff2<br>
./src/main/webapp/resources/js/bootstrap.min.js<br>
./src/main/webapp/resources/js/jquery.min.js<br>
./src/main/webapp/WEB-INF/web.xml<br>
./src/test/java/com/dea42/genSpring/controller/ApiControllerTest.java<br>
./src/test/java/com/dea42/genSpring/controller/AppControllerTest.java<br>
./src/test/java/com/dea42/genSpring/controller/Sheet1ControllerTest.java<br>
./src/test/java/com/dea42/genSpring/controller/Sheet2ControllerTest.java<br>
./src/test/java/com/dea42/genSpring/MockBase.java<br>
./src/test/java/com/dea42/genSpring/selenium/SeleniumBase.java<br>
./src/test/java/com/dea42/genSpring/selenium/SmokeIT.java<br>
./src/test/java/com/dea42/genSpring/selenium/SmokeTest.java<br>
./src/test/java/com/dea42/genSpring/UnitBase.java<br>
./src/test/java/com/dea42/genSpring/WebAppApplicationTest.java<br>

#Basic structure is
## basic pom file
./pom.xml <br>
## DB Dialect if needed.
./src/main/java/com/dea42/genSpring/db/SQLiteDialect.java  **(Only if using SQLite)**<br>
## Language files. Note currently just these 3 UTF-8 ones and only common text is translated. To translate your table and field names use https://www.google.com/search?q=translate+java+properties+file with untranslated properties from ./src/main/resources/messages.properties<br> 
./src/main/resources/messages.properties<br>
./src/main/resources/messages_de.properties<br>
./src/main/resources/messages_fr.properties<br>
## page with links API URLs
./src/main/resources/templates/api_index.html
## item edit / create pages
./src/main/resources/templates/edit_*.html<br>
## item list pages
./src/main/resources/templates/*s.html<br>
## sample static web pages
./src/main/webapp/public/optView.html ** sample static web page**<br>
./src/main/webapp/public/Players.html ** sample static web page**<br>
./src/main/webapp/public/resources/sheet.css ** css file for Google sheet tabs exported as HTML**<br>

## basic web app framework pages
./src/main/resources/templates/error/general.html<br>
./src/main/resources/templates/fragments/alert.html<br>
./src/main/resources/templates/fragments/footer.html<br>
./src/main/resources/templates/fragments/header.html **(nav header)** <br>
./src/main/resources/templates/home/homeNotSignedIn.html<br>
./src/main/resources/templates/home/homeSignedIn.html<br>
./src/main/resources/templates/home/signin.html<br>
./src/main/resources/templates/home/signup.html<br>
./src/main/resources/templates/index.html<br>
./src/main/webapp/favicon.ico ** sample site icon**<br>
## sample css files
./src/main/webapp/resources/css/bootstrap.min.css<br>
./src/main/webapp/resources/css/site.css<br>
## needed Javascript
./src/main/webapp/resources/js/bootstrap.min.js<br>
./src/main/webapp/resources/js/jquery.min.js<br>
## needed fonts and icons
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.eot<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.svg<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.ttf<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.woff<br>
./src/main/webapp/resources/fonts/glyphicons-halflings-regular.woff2<br>
## app url mapping file
./src/main/webapp/WEB-INF/web.xml<br>
<br>
## Unit test files<br>
./src/test/java/com/dea42/genSpring/*<br>
./src/test/resources <br>
 
 Use the integration maven profile to run test of a cargo deployed version of the app
 
# Running the web app
- Import generated project as existing maven project<br>
- Do maven install ** Note this runs a basic test of the build as well by creating to sample projects and building them**<br>
- Run ../genSpringTest/src/main/java/com/dea42/genSpring/WebAppApplication.java as Java or Spring Boot app<br>
- Wait for start up<br>
- Point browser to  http://localhost:8080/

