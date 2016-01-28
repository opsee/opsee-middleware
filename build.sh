#!/bin/sh

protoc --plugin=protoc-gen-grpc-java=/usr/local/bin/protoc-gen-grpc-java \
        --proto_path=/usr/local/include \
        --java_out=src --grpc-java_out=src --proto_path=resources/proto /usr/local/include/github.com/gogo/protobuf/gogoproto/gogo.proto
protoc --plugin=protoc-gen-grpc-java=/usr/local/bin/protoc-gen-grpc-java \
        --proto_path=/usr/local/include \
        --java_out=src --grpc-java_out=src --proto_path=resources/proto resources/proto/checker.proto
protoc --plugin=protoc-gen-grpc-java=/usr/local/bin/protoc-gen-grpc-java \
        --proto_path=/usr/local/include \
        --java_out=src --grpc-java_out=src --proto_path=resources/proto resources/proto/aws.proto
