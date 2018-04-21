# Rolar
S3 log reader and writer

This project takes the log files that we write to S3, stitches them together, and then formats the output. It is Kotlin Spring Boot with React for the frontend.

## API

* Logs can be read by getting them.

`Get`: `http://localhost:8080/api/logs/staging/host1`

* Use the the 'after' request parameter to see all logs after a certain timestamp.

`Get`: `http://localhost:8080/api/logs/staging/host1?after=152417245574`

* A reactive input sends events as the logs come in until the log is closed. This endpoint can optionally use the 'after' request parameter.

`Get`: `http://localhost:8080/api/reactive/logs/staging/host1`

* Logs can be written by posting them.

`Post`: `http://localhost:8080/api/logs/staging/host1`
```
{
    "host": "Clays_MBP",
    "content": [
    	{
    		"level": "info",
    		"message": "moar logs",
    		"timestamp": "2018-04-20 22:35:18.743"
    	}
    ]
}
```
The path after `/api/logs/` is the folder where the logs with be written inside the S3 folder. It doesn't matter what format is used for timestamp in the post body, it is just a String. A timestamp will be returned for the most recent logs. Using this timestamp as the 'after' when reading logs will return no logs unless something has been added since.

* When you are done writing to the log location, close it with the 'closed' request parameter containing any value.

`Post`: `http://localhost:8080/api/logs/staging/host1?closed=true`
```
{
    "host": "Clays_MBP",
    "content": []
}
```
The content can contain the last set of log messages.

## UI
React is used to render a view of the log that is being tailed until the log is closed. This page is also an example of how the reactive log endpoint can be used. The 'after' request parameter can be used here too.

`http://localhost:8080/logs/staging/host1`

## Running it

It can be run like a normal Spring Boot project and this builds the JS too.
`./mvnw spring-boot:run`

Be sure to set these environment variables so that the AWS creds have read access to the specified bucket and sub folders.
* AWS_ACCESS_KEY_ID
* AWS_SECRET_ACCESS_KEY
* S3_LOGGING_BUCKET
