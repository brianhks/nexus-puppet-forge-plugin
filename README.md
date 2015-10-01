# Nexus Puppet Forge Plugin
## Summary
This plugin adds Puppet Forge API v3 rest api's to a Maven 2 Nexus repository.

## Uploading Puppet Modules
The paths and naming conventions used must all match the maven 2 convention.
The reason for this is that this plugin utilizes some of the features of the 
maven 2 repository.

Each module must have a metadata.json file and be compressed as a tar.gz file.
Lets say I want to upload the mysql module from puppet labs (puppetlabs-mysql-3.6.1.tar.gz),
In order to upload the module to nexus I must conform to the naming convention 
of mavent 2.  Here is a sample curl statement to upload the module

```
curl -v -u $NEXUS_USER:$NEXUS_PASSWORD --upload-file puppetlabs-mysql-3.6.1.tar.gz  $NEXUS_URL/nexus/content/repositories/$NEXUS_REPOSITORY/puppetlabs/mysql/3.6.1/mysql-3.6.1.tar.gz
```

The $NEXUS_REPOSITORY name must have the word puppet in it.  This is part of what
the plugin triggers off of to parse the module.  If the file ends with tar.gz
and the repository has the word puppet in it this plugin will extract the metadata.json
from the tar and saves it in the same folder as the module.  The plugin will also
parse the metadata.json file and generate a pom.xml file.

For reference look at this project I created: https://github.com/brianhks/puppet-module-publisher
It is a set of scripts for downloading modules using librarian-puppet and then
there is a script to upload them into Nexus.

## Using Librarian Puppet to download from Nexus
At the top of your Puppetfile set this value
```
forge "http://nexus:8081/nexus/service/siesta/puppetforge/puppet"
```
Just replace /puppet on the end with the name of your repository.  When you call
librarian-puppet make sure to pass --no-use-v1-api so it won't try to use version
1 of the api which is not supported by this plugin.