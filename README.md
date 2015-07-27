# XmlDiffer #

XmlDiffer is a Java project to compare two SAML XML metadata files with each other for changes. Currently it is specifically aimed to serve [Haka federation](https://confluence.csc.fi/x/CYvAAQ), but may be of use also for other SAML based federations with minor modifications.

The project is on development status and should not be considered as production ready.

### Requirements ###

* [xmlunit](http://www.xmlunit.org)
* Apache Commons
    * [codec](https://commons.apache.org/proper/commons-codec/)
    * [fileUpload](https://commons.apache.org/proper/commons-fileupload/)
        * [io](https://commons.apache.org/proper/commons-io/)
* [json.org](http://mvnrepository.com/artifact/org.json/json)
* [Jetty](http://www.eclipse.org/jetty/documentation/9.2.8.v20150217/advanced-embedding.html#downloading-jars)

### Set up ###

Project is supposed to be run in embedded Jetty server. The code will be compiled to a single jar package. Running the JettyExperiment class main method will launch the diff service within standalone Jetty server. Practically, a proxy is needed in front of the server. Use Apache ProxyPass or similar.

The package may also be driven from the command line with arguments.

### What is this federation thing all about? ###

* what is a [SAML](https://www.oasis-open.org/committees/download.php/13525/sstc-saml-exec-overview-2.0-cd-01-2col.pdf)
* what is [Haka](https://confluence.csc.fi/x/CYvAAQ)

If you find something to change, add an issue or create a pull request.