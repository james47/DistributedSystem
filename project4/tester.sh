#!/bin/bash

for I in {1..50}
do
echo "Test "$I  
java BlockchainTest Initial-State 9111
java BlockchainTest Basic-Mining 9111
java BlockchainTest Basic-MultiMining 9111
java BlockchainTest Consensus 9111
java BlockchainTest Reconnect-Consensus 9111

done
