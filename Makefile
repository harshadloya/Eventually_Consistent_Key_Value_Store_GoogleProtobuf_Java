LIB_PATH=/home/phao3/protobuf/protobuf-3.4.0/java/core/target/protobuf.jar
target: clean controller branch
	mkdir bin
	javac -classpath $(LIB_PATH) -d bin src/*.java


clean:
	rm -rf bin/
	rm -rf controller
	rm -rf branch
	rm -rf write*.txt
	rm -rf hint*.txt
	
controller:
	@echo "Writing shortcut script controller ..."
	@echo '#!/bin/bash +vx' > controller
	@echo 'LIB_PATH=$$"/home/phao3/protobuf/protobuf-3.4.0/java/core/target/protobuf.jar"' >> controller
	@echo 'java -classpath bin:$$LIB_PATH Controller "$$@"' >> controller
	@chmod +x controller

branch:
	@echo "Writing shortcut script branch ..."
	@echo '#!/bin/bash +vx' > branch
	@echo 'LIB_PATH=$$"/home/phao3/protobuf/protobuf-3.4.0/java/core/target/protobuf.jar"' >> branch
	@echo 'java -classpath bin:$$LIB_PATH Branch "$$@"' >> branch
	@chmod +x branch