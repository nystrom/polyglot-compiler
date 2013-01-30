#!/bin/bash

cd ../..
ant jar jar-ppg
cp ./lib/polyglot.jar ./lib/ppg.jar src/generics/lib