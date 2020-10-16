cd back-end
mvn package
cp target/back-end-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../ips-backend.jar
cp -R lib/ ../lib
cp -R example-generation/ ../input-generator
cd ..
zip -r ips.zip lib/ ips-backend.jar front-end/ input-generator/
rm -r lib/
rm -r input-generator/
rm ips-backend.jar