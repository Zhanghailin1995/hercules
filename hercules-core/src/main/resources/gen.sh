#!/bin/bash
protoc -I=./  --descriptor_set_out=rpc.desc --java_out=../java/ rpc.proto