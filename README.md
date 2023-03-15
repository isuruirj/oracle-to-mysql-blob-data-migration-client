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
