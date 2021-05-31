#!/bin/bash

echo
java CryptoMain init $1
echo
java CryptoMain put $1 $2 $3
echo
java CryptoMain get $1 $2 
echo
