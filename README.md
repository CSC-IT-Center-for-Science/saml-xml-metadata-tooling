[![Build Status](https://travis-ci.org/CSC-IT-Center-for-Science/saml-xml-metadata-tooling.svg?branch=xmltooling-dev)](https://travis-ci.org/CSC-IT-Center-for-Science/saml-xml-metadata-tooling)
[![Coverage Status](https://coveralls.io/repos/github/CSC-IT-Center-for-Science/saml-xml-metadata-tooling/badge.svg)](https://coveralls.io/github/CSC-IT-Center-for-Science/saml-xml-metadata-tooling)
# SAML XML Metadata Tooling

This project aims to create a set of tools for SAML metadata handling. First aim
is to define microservices to validate the metadata, notify changes and aid in
publishing the metadata to publication platforms. The source for the metadata
is operator's current tools to generate metadata files.

Work it is specifically aimed to serve
[Virtu](https://wiki.eduuni.fi/display/CSCVIRTU/Virtu) and
[Haka federations](https://wiki.eduuni.fi/display/CSCHAKA),
but may be of use also for other SAML based federations with minor modifications.

This is a work in progress and should not be considered as production ready as is.

### Requirements ###

* [Spring-boot](https://projects.spring.io/spring-boot/)
* [xmlunit](http://www.xmlunit.org)

