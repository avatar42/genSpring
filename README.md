# Properties that may be used by both programs
## DB properties
### DB to create tables in (note if blank and db.driver=org.sqlite.JDBC creates a SQLite DB in genSpring.outdir folder. See Db.getUrl(ResourceBundle bundle, String folder))
db.url=jdbc:sqlite:L:/sites/git/genSpring/target/watchlistDB.sqlite 
### DB driver to use
db.driver=org.sqlite.JDBC 
### add these if needed
db.user= <br>
db.password= <br>
db.name= <br>

## keyed on Entity Class name
### className.key note list values are the field names but are compared case insensitive to avoid mismatches 
### columns not to return in REST interface
Account.JsonIgnore=password
### columns that need unique values
Account.unique=email
## restrict columns on list page to just these
Account.list=id,email,role,created

### tableName.key normal test values See Sheets2DBTest and Sheet2AppTest for examples of use
Sheet1.testCols=5<br>
Sheet2.testCols=5<br>
Sheet1.testRows=8<br>
Sheet2.testRows=8<br>
### if some columns in Sheet1 flagged as user columns these would also be expected
Sheet1User.testCols=5<br>
Sheet1User.testRows=8<br>

# Sheet2DB
Creates a DB from a Google sheet with a table for each selected tab <br>
 
# Setup
Basically set your options in /genSpring/src/main/resources/genSpring.properties <br>
** Note after a clean you will be asked to auth to Google to access the sheet. See target/tokens folder** 
 
## optional SQLite db output location
genSpring.outdir=../genSpringTest
## Google sheet to read from
genSpring.id=1-xYv1AVkUC5J3Tqpy2_3alZ5ZpBPnnO2vUGUCUeLVVE 
## tabs to export (values can be tab or table names) 
genSpring.tabs=Networks,shows,Roamio_npl,Roamio_sp,Roamio_Todo,OTA,CableCard 
 
### [optional] Tabs each row should be prefixed / linked to a user ID. ID 1 is used for the import.
genSpring.userTabs=Roamio_npl,Roamio_sp,Roamio_Todo,OTA,CableCard<br>

## tab adjustments (note table names used since tab names can have spaces in the,
### tableName.columns to export. If missing exports all existing
Sheet1.columns=A-C,F
Sheet2.columns=A,C-E

### tableName.required (not null) columns. Rows with null in these columns will not be imported. Note ignored if not in exported columns
Sheet1.required=A
Sheet2.required=A,C-D

### [optional] tableName.lastRow set this if you need to ignore extra rows at bottom of sheet
Sheet1.lastRow=251 <br>

### [optional] tableName.Columns to be placed in separate table linked to source table and account. 
Sheet1.user=E<br>

# genSpring
Create a simple Spring boot CRUD app with both web and REST interfaces from a DB complete with basic hard coded, in mem, auth system.
Tested with SQLite but code was brought over from Struts version for MySQL and SQLServer that will probably work.
Note for safety reasons it will not overwrite existing files so just delete the files you want to regen and run the tools again.

# Setup
Basically set your options in /genSpring/src/main/resources/genSpring.properties 
 
## top folder for output / new project root 
genSpring.outdir=../genSpringTest 
## used if not empty. Current year added to end.
genSpring.Copyright=Copyright (c) 2001- 
## ignored if blank
genSpring.Company=RMRR 
## defaults to GenSpring.genSpringVersion
genSpring.version= 
## base package / groupId
genSpring.pkg=com.dea42 
## sub package / artifactId
genSpring.module=genSpring 
## Optional if the artifactId needs to be diff from module. Defaults to same as module.
genSpring.artifactId=genSpringTest
## add a toString() to the entity bean class
genSpring.beanToString=true
## add a equals() and hashCode() to the entity bean and form classes ( now always true so mock testing can work)
genSpring.beanEquals=true
## use Double instead of BigDecimal for entities beans.
genSpring.useDouble=true
## Filter tables to gen code for by name
genSpring.filteredTables=providers
## port for regression test Tomcat to use
genSpring.tomcatPort=8089

# Running the generator
Then run /genSpring/src/main/java/com/dea42/build/GenSpring.java as a console Java app. <br>
		USAGE: Genspring [options] [table names]<br>
		Where options are:<br>
		-double = use Double instead of BigDecimal for entities beans **(also genSpring.useDouble in properties file)**<br>
		~~-toString = generate toString() methods for entities beans **(Always true now)**~~<br>
		~~-beanEquals = generate equals() methods for entities beans **(Always true now)**~~<br><br>
		<br>
		if table names not given then runs on all tables in DB.<br>
		<br>
		Note: be sure to set properties in resources/genSpring.properties before running.<br>

# Output from generator
For example with a DB containing the tables Sheet1 and Sheet2 would generate [these files](https://github.com/avatar42/genSpringTest/blob/master/files.md) <br>

#Basic structure is
## Basic pom file
./pom.xml <br>
## DB Dialect if needed.
./src/main/java/{pkg.module}/db/SQLiteDialect.java  **(Only if using SQLite)**<br>
## Language files. Note currently just these 3 UTF-8 ones and only common text is translated. To translate your table and field names use https://www.google.com/search?q=translate+java+properties+file with untranslated properties from ./src/main/resources/messages.properties<br> 
./src/main/resources/messages.properties<br>
./src/main/resources/messages_de.properties<br>
./src/main/resources/messages_fr.properties<br>
## Page with links API URLs
./src/main/resources/templates/api_index.html
## Item edit / create pages
./src/main/resources/templates/edit_*.html<br>
## item list pages
./src/main/resources/templates/*s.html<br>
## Sample static web pages
./src/main/webapp/public/optView.html ** sample static web page**<br>
./src/main/webapp/public/Players.html ** sample static web page**<br>
./src/main/webapp/public/resources/sheet.css ** css file for Google sheet tabs exported as HTML**<br>

## Basic web app framework pages
./src/main/resources/templates/*.html<br>
./src/main/webapp/favicon.ico ** sample site icon**<br>
## Sample css files
./src/main/webapp/resources/css/bootstrap.min.css<br>
./src/main/webapp/resources/css/site.css<br>
## Needed Javascript
./src/main/webapp/resources/js/bootstrap.min.js<br>
./src/main/webapp/resources/js/jquery.min.js<br>
## Needed fonts and icons
./src/main/webapp/resources/fonts/*<br>
## App URL mapping file
./src/main/webapp/WEB-INF/web.xml<br>
<br>
## Unit test files<br>
./src/test/java/{pkg.module}/WebAppApplicationTest.java<br>
./src/test/java/{pkg.module}/controller/*ControllerTest.java<br>
./src/test/java/{pkg.module}/search/*SearchTest.java<br>
### Common test methods
./src/test/java/{pkg.module}/UnitBase.java<br>
### Methods for mock testing
./src/test/java/{pkg.module}/MockBase.java<br>

## Regression tests
### Methods for Selenium regression tests
./src/test/java/{pkg.module}/selenium/SeleniumBase.java<br>
### Runs regression tests against deployed war
./src/test/java/{pkg.module}/selenium/SmokeIT.java<br>
### Runs regression tests against Sprint Boot standalone config
./src/test/java/{pkg.module}/selenium/SmokeTest.java<br>
### Properties files used for testing.
./src/test/resources/rename.properties<br>
./src/test/resources/test.properties<br>
 
Use the integration maven profile to run test of a cargo deployed version of the app
 
# Running the web app
- Import generated project as existing maven project<br>
- Do maven install ** Note this runs a basic tests of the build as well**<br>
- Run ../genSpringTest/src/main/java/com/dea42/genSpring/WebAppApplication.java as Java or Spring Boot app<br>
- Wait for start up<br>
- Point browser to  http://localhost:8080/

