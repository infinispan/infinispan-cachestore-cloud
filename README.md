# Infinispan Cloud Cache Store

## Documentation
For more information, please refer to [the documentation of this cache store](documentation/src/main/asciidoc/index.adoc).

## Running tests
### Amazon AWS
`mvn clean verify -Paws-s3 -Dinfinispan.test.jclouds.username=$S3_ACCESS_KEY -Dinfinispan.test.jclouds.password=$S3_SECRET_KEY`

### OpenStack Swift
`mvn clean verify -Pswift -Dinfinispan.test.jclouds.username=$SWIFT_TENANT_NAME:$SWIFT_USER_NAME -Dinfinispan.test.jclouds.password=$SWIFT_PASSWORD -Dinfinispan.test.jclouds.endpoint=$SWIFT_ENDPOINT_URL`
