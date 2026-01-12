awslocal s3api create-bucket \
--bucket police-emails \
--region eu-west-2 \
--create-bucket-configuration '{"LocationConstraint": "eu-west-2"}'