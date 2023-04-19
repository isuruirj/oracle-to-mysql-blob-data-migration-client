# oracle-to-mysql-blob-data-migration-client

A java client designed to migrate blob data from Oracle database to MySQL database.

### Build
- Build the component by running "mvn clean package"
- This will create two jars in /target directory.
  - db.blob.migration-1.0.jar
  - db.blob.migration-1.0-jar-with-dependencies.jar
  
### Configurations
- Provide oracle database parameters and mysql database parameters accordingly in config.properties file.
- CLIENT.OPERATION - set IMPORT or EXPORT based on the operation requiried to perform
- EXPORT.DIRECTORY.PATH - client will export the dump to provided path, shourd provide read/write permissions to ths path.
- IMPORT.DIRECTORY.PATH - client will import the dump from this path (mostly this's same as export path)

### Run

java -jar db.blob.migration-1.0-jar-with-dependencies.jar

### To Export
- Copy the db.blob.migration-1.0-jar-with-dependencies.jar and config.properties file to the same directory.
- Update connection parameters for both Oracle and MySQL databases.
- Set CLIENT.OPERATION to EXPORT.
- Set the EXPORT.DIRECTORY.PATH where export should get stored and at the end append /oracleToMysql (don't create /oracleToMysql directory, it'll get created from the client).
- Set the IMPORT.DIRECTORY.PATH where data will be reading during the import process and at the end append /oracleToMysql.
- Run the client.
- It'll create the oracleToMysql directory in the provided export path and put the dumps there.

### To Import
- Copy the db.blob.migration-1.0-jar-with-dependencies.jar and config.properties file to the same directory.
- Set CLIENT.OPERATION to IMPORT.
- Copy the /oracleToMysql directory which got generated during export to the IMPORT path provided.
- Run the client.
