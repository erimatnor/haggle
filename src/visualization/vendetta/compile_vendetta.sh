#!/bin/bash

javac -d classes $(find . -name "*.java" | grep -v "(A Document" | grep -v "\._")