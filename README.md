# doors-exercise

## Pre-requisites
Project is built using **SBT**, so please install SBT version >= 1 

Flink cluster with Kafka is containerized so latest available **Docker** needed as well.
 
## To run

#### Run in Docker

Build and package the Flink job to a fat jar:

`sbt package`

Start docker-compose with Flink and Kafka:

`docker-compose up -d`

Get the id of Flink job manager container:

`JOBMANAGER_CONTAINER=$(docker ps --filter name=flink-jobmanager --format={{.ID}})`


Copy the jar to the container:

`docker cp target/scala-2.12/doors-exercise_2.12-0.1-SNAPSHOT.jar "$JOBMANAGER_CONTAINER":/job.jar`


Run the job 

`docker exec -t -i "$JOBMANAGER_CONTAINER" flink run /job.jar`

Check the execution in Flink UI : 

`http://localhost:8081/#/overview`


#### Run the job in SBT

To run and test your application locally, you can just execute `sbt run` then select the main class that contains the Flink job . 

You can also package the application into a fat jar with `sbt assembly`, then submit it as usual, with something like: 

```
flink run -c com.doors.WordCount /path/to/your/project/my-app/target/scala-2.11/testme-assembly-0.1-SNAPSHOT.jar
```