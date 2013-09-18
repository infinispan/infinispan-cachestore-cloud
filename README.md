# Infinispan Cloud Cache Store

## Documentation
For more information, please refer to [the documentation of this cache store](documentation/src/main/asciidoc/index.adoc).

## Running tests
### Amazon AWS
`mvn -Pintegration test -Dinfinispan.test.jclouds.username=$S3_USER -Dinfinispan.test.jclouds.password=$S3_PWD -Dinfinispan.test.jclouds.service=aws-s3`

### Rackspace
`mvn -Pintegration test -Dinfinispan.test.jclouds.username=$RACKSPACE_USER -Dinfinispan.test.jclouds.password=$RACKSPACE_PWD -Dinfinispan.test.jclouds.service=cloudfiles-us`

### Azure
`mvn -Pintegration test -Dinfinispan.test.jclouds.username=$AZURE_USER -Dinfinispan.test.jclouds.password=$AZURE_PWD -Dinfinispan.test.jclouds.service=azureblob`
