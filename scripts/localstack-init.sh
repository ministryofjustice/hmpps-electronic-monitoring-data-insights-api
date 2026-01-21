awslocal s3api create-bucket \
--bucket emdi-data-storage \
--region eu-west-2 \
--create-bucket-configuration '{"LocationConstraint": "eu-west-2"}'