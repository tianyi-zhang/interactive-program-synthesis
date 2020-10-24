cd back-end
mvn package
cp target/back-end-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../ips-backend.jar
cp -R lib/ ../lib
cd ..
zip -r ips.zip lib/ ips-backend.jar front-end/ back-end/ 
zip -j ips.zip lib/libz3java.dylib lib/com.microsoft.z3.jar lib/libz3.dylib lib/libz3java.so
rm -r lib/
rm ips-backend.jar