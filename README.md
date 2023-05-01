# AERIUS File Server

The file server manages access to files within AERIUS.
This is both files used internally to communicate between services as well as files provided to the user.
The file server is not intended to be directly accessible externally.
External access is provided via Connect.

The file server has two modes: Local and Amazon s3
Local mode means the files are stored locally to the file server.
Amazon s3 means the files are stored in Amazon S3.
Only 1 mode should be active.
A mode is enabled by passing it as a profile when starting the file server:

For local files use:

```
spring.profiles.active=local
```

For Amazon S3 files use:

```
spring.profiles.active=s3
```

Each file server mode has different configuration parameters.
The following parameters are available for each mode.

### Local Files

Local file configuration parameters are:

### preventCleanup

Enable files from ever being deleted.
Should only be used in develop environments.

```
aerius.file.storage.preventCleanup=[true|false]
```

#### location

The location to store the files.
This location should survive system restarts.

```
aerius.file.storage.location=<directory>
```

### Amazon S3

Amazon S3 file configuration parameters are:

#### bucketName

The name of the Amazon S3 bucket the files are stored under.
The bucket is _NOT_ created by the file server itself and should exist when started.
Preferable these buckets are created for the specific environment the application is used.
Like production, acceptance, test environment.

```
aerius.file.storage.s3.bucketName=dev`
```

#### Access credentials and region

AWS S3 credentials and region can be configured as environment variables.
The following environment variables need to be set:

```
AWS_ACCESS_KEY_ID=<access key id>
AWS_REGION=<region>
AWS_SECRET_ACCESS_KEY=<secret access key>
```

For more information on credentials configuration see the Amazon documentation: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/get-started.html#get-started-setup-credentials
