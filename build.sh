#!/bin/sh

echo "*** Building frontend"
cd frontend
npm run build
rm -rf ../src/main/resources/htdocs/*; cp -rf ./build/* ../src/main/resources/htdocs/

cd ..

echo "*** Building backend"
mvn clean install


echo "*** Building done"
