# doors-exercise

## Pre-requisites
Project is built using **SBT**  >= 1 

Flink cluster with Kafka is containerized so latest available **Docker** is needed as well.
 
## To run

#### Run in Docker

Build and package the Flink job to a fat jar:

`sbt assembly`

Start docker-compose with Flink and Kafka:

`docker-compose up -d`

Get the id of Flink job manager container:

`JOBMANAGER_CONTAINER=$(docker ps --filter name=flink-jobmanager --format={{.ID}})`


Copy the jar to the container:

`docker cp target/scala-2.12/doors-exercise-job.jar "$JOBMANAGER_CONTAINER":/job.jar`

Run the job 

`docker exec -t -i "$JOBMANAGER_CONTAINER" flink run /job.jar`

Check the execution in Flink UI : 

`http://localhost:8081/#/overview`


#### Consult produced stats
To check produced stats  in Kafka:
`docker exec -t -i $(docker ps --filter name=doors-kafka --format={{.ID}}) /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic doors-stats
` 


#### Run the job in SBT

We still need to provide Kafka input/output topics for the job, to run Kafka only (without Flink containers):
```
docker-compose up -d -f docker-compose-only-kafka.yml up
```
Then to run and test your application locally, you can just execute `sbt run` then select the main class that contains the Flink job . 


 
## Door stream
We use event time as stream time characteristic
``` 
val env = StreamExecutionEnvironment.getExecutionEnvironment
env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
```

### Domain

Main abstraction is a `DoorEvent` which looks like this:
```
final case class DoorEvent(doorId: Int, eventType: DoorEventType, timestamp: Long)
```
When events are persisted to Kafka topic they are serialized to Json format (see `com.doors.utils.Json` for details)
 

### Doors stream input
Doors are generated and pushed to input Kafka topic `doors` which is used as input source for main streaming flow in `DoorsJob`.

I used the same job to generate the input source, in real-world scenario another job or producer would have been used. 

#### Doors Generator
I use simple door generator which picks random door from 200 available and assigns random IN/OUT event and timestamp with random delay within a limit.

`DoorEventSource` function emits generated  events in order (no late event processing in the main stream).


#### Kafka sink for generated doors
All door events are streamed to Kafka sink, to topic `doors`


 
### Main door processing stream flow
#### reading events from Kafka
We basically try to deserialize Json events and filter out errors.

```
val doorsStream =
      env.addSource(source).name("Doors input stream")
        .map(toDoorEvent)
        .filter(_.isSuccess)
        .map(_.get)
        .assignAscendingTimestamps(_.timestamp)
```

Since events don't come out-of-order we can assign ascending timestamps and watermarks.


#### counting per door and producing basic stats
We group   events by `doorId` field using tumbling window of 10 secs and preagregate counting types of event to : 
``` 
final case class PerDoorCounts(doorId: String, count: Int, inCount: Int, outCount: Int)

  val countPerDoorStream = doorsStream
      .keyBy(_.doorId)
      .timeWindow(WINDOW_SIZE)
      .aggregate(new PerDoorAggregation)

```

then we can count simple stats per doors grouping all streams by window of 10 secs
```
    val allPerDoorCounts = countPerDoorStream.timeWindowAll(WINDOW_SIZE)
    
    // example of one of the stats
    allPerDoorCounts
      .maxBy("count")
      .map(counts => DoorStats[PerDoorCounts](StatsType.door_most_used, counts))
      .addSink(doorStatsSink)
```

        
#### output basic stats per door
Door stats are pushed to another Kafka topic `doors-stats` in Json format.

Basic door stats case class :
`final case class DoorStats[T<:DoorStatsValue](key: StatsType, value: T)


#### compute total stats across all doors
We reuse aggregated stream with cound per door from previous steps:

` val allPerDoorCounts = countPerDoorStream.timeWindowAll(WINDOW_SIZE)`

then we need to aggregate events per period (window start-end)
to be able to produce stats correspondig to this interface : 
```
final case class TotalCounts(period: Period, count: Int) 
```

using reduce function to inkect window infos:
```
allPerDoorCounts
      .reduce(
        (ev1: PerDoorCounts, ev2: PerDoorCounts) => ev1.copy(count = ev1.count + ev2.count),
        (
          window: TimeWindow,
          counts: Iterable[PerDoorCounts],
          out: Collector[TotalCounts]) => {
          out.collect(TotalCounts(Period(window.getStart, window.getEnd), counts.iterator.next().count))
        }
      )
```

with the result stream we can count min/max events across different windows. Using here sliding window of 1 minute compiuted every 10 secs:

```
// one stats as example
 .windowAll(SlidingEventTimeWindows.of(TOTAL_STATS_WINDOW_SIZE, TOTAL_STATS_WINDOW_SLIDE))
      .minBy("count")
      .map(counts => DoorStats[TotalCounts](StatsType.less_busy_window, counts))
```
#### output total  stats
Stats are pushed to  Kafka topic `doors-stats` in Json format.


## TODO
- UI push stats to Elastic Search + Kibana for visualization
- CEP with door patterns
- Testing
