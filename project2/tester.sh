#!/bin/bash

port=31000

for I in {1..30}
do

java RaftTest Initial-Election $((port + I * 8 + 0))
java RaftTest Re-Election $((port + I * 8 + 1))
java RaftTest Basic-Agree $((port + I * 8 + 2))
java RaftTest Fail-Agree $((port + I * 8 + 3))
java RaftTest Fail-NoAgree $((port + I * 8 + 4))
java RaftTest Rejoin $((port + I * 8 + 5))
java RaftTest Backup $((port + I * 8 + 6))
java RaftTest Count $((port + I * 8 + 7))

done
