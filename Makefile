# See README.txt.

.PHONY: all cpp java python clean

all: cpp java python

cpp:    add_person_cpp    list_people_cpp
java:   controller   branch
python: add_person_python list_people_python

clean:
	rm -f controller 
	rm -f javac_middleman Bank*.class Branch*.class Controller*.class FileProcessor.class Hints.class
	rm -f protoc_middleman bank.pb.cc bank.pb.h bank_pb2.py Bank.java
	rm -f *.pyc
	rm -f protoc_middleman_go tutorial/*.pb.go add_person_go list_people_go
	rmdir tutorial 2>/dev/null || true
	rmdir com/example/tutorial 2>/dev/null || true
	rmdir com/example 2>/dev/null || true
	rmdir com 2>/dev/null || true

protoc_middleman: bank.proto
	protoc --cpp_out=. --java_out=. --python_out=. bank.proto
	@touch protoc_middleman

javac_middleman: Controller.java protoc_middleman
	javac -cp /home/phao3/protobuf/protobuf-3.4.0/java/core/target/protobuf.jar FileProcessor.java Bank.java Branch.java Controller.java Branches.java Hints.java
	@touch javac_middleman

controller: javac_middleman
	@echo "Writing shortcut script controller ..."
	@echo '#! /bin/sh' > controller
	@echo 'java -classpath .:$$CLASSPATH Controller "$$@"' >> controller
	@chmod +x controller

branch: javac_middleman
	@echo "Writing shortcut script branch ..."
	@echo '#! /bin/sh' > branch
	@echo 'java -classpath .:$$CLASSPATH Branch "$$@"' >> branch
	@chmod +x branch

