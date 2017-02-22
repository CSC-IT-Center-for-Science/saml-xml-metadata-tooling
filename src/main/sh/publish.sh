#!/bin/bash

targetFile=virtu-metadata-test-2.xml
hosts=( "virtu-pubhost01.csc.fi" "virtu-pubhost02.csc.fi" )

for host in "${hosts[@]}"
do
    echo "copying file $1 to ${targetFile} on ${host}"
    scp -4i /opt/saml-tooling/id_rsa $1 virtu_pubuser@${host}:/var/www/ds/fed/virtu/${targetFile} 2>1
    if [ $? -ne 0 ]; then
        echo "ERROR copying"
        exit 1
    fi
done


echo "end"