# genSpring
Create a simple Spring boot CRUD app with both web and REST interfaces from a DB complete with basic hard code in mem auth system.
Tested with SQLite but code was brought over from Struts version for MySQL and SQLServer that will probably work.

# Setup
Basically set you options in /genSpring/src/main/resources/genSpring.properties

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
For example with a DB containing the tables Networks, Providers and Shows would generate the files
./pom.xml
./src/main/java/com/dea42/watchlist/controller/ApiController.java
./src/main/java/com/dea42/watchlist/controller/AppController.java
./src/main/java/com/dea42/watchlist/controller/NetworksController.java
./src/main/java/com/dea42/watchlist/controller/ProvidersController.java
./src/main/java/com/dea42/watchlist/controller/ShowsController.java
./src/main/java/com/dea42/watchlist/db/SQLiteDialect.java
./src/main/java/com/dea42/watchlist/entity/Networks.java
./src/main/java/com/dea42/watchlist/entity/Providers.java
./src/main/java/com/dea42/watchlist/entity/Shows.java
./src/main/java/com/dea42/watchlist/repo/NetworksRepository.java
./src/main/java/com/dea42/watchlist/repo/ProvidersRepository.java
./src/main/java/com/dea42/watchlist/repo/ShowsRepository.java
./src/main/java/com/dea42/watchlist/SecurityConfiguration.java
./src/main/java/com/dea42/watchlist/service/NetworksServices.java
./src/main/java/com/dea42/watchlist/service/ProvidersServices.java
./src/main/java/com/dea42/watchlist/service/ShowsServices.java
./src/main/java/com/dea42/watchlist/WebAppApplication.java
./src/main/resources/application.properties
./src/main/resources/static/css/site.css
./src/main/resources/templates/edit_networks.html **(edit / create page)**
./src/main/resources/templates/edit_providers.html
./src/main/resources/templates/edit_shows.html
./src/main/resources/templates/index.html **(links to pages below plus /api)**
./src/main/resources/templates/login.html
./src/main/resources/templates/networkss.html **(list page)**
./src/main/resources/templates/providerss.html
./src/main/resources/templates/showss.html

and the folders
./src/test/java
./src/test/resources

# Running the web app
- Import generated project as existing maven project
- Do maven install
- Run /gen/src/main/java/com/dea42/watchlist/WebAppApplication.java as Java or Spring Boot app
- Wait for start up
- Point browser to  http://localhost:8080/

