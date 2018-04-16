# Rolar
S3 log viewer

This project takes the log files that we write to S3 and stitches them together then formats the output. It is Kotlin Spring Boot with React for the frontend.

It can be run like a normal Spring Boot project and this builds the JS too.
`./mvnw spring-boot:run`

Be sure to set these environment variables so that the AWS creds have read access to the specified bucket and sub folders.
AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and S3_LOGGING_BUCKET
