#!/bin/bash

echo "========================================"
echo "Testing Reliable Transport Protocols"
echo "========================================"

# Compile the code
echo "Compiling..."
javac *.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Test parameters
NUM_MESSAGES=20
WINDOW_SIZE=8
TIMEOUT=30.0
SEED=1234
MEAN_TIME=100.0

echo ""
echo "Test Configuration:"
echo "  Number of messages: $NUM_MESSAGES"
echo "  Window size: $WINDOW_SIZE"
echo "  Timeout: $TIMEOUT"
echo "  Random seed: $SEED"
echo "  Mean time between messages: $MEAN_TIME"
echo ""

# Function to run a test
run_test() {
    PROTOCOL=$1
    LOSS=$2
    CORRUPTION=$3
    TRACE=$4
    OUTPUT_FILE=$5
    
    echo "Running $PROTOCOL with loss=$LOSS, corruption=$CORRUPTION..."
    
    # Create input for the simulator
    cat > input.txt << EOF
$NUM_MESSAGES
$LOSS
$CORRUPTION
$MEAN_TIME
$WINDOW_SIZE
$TIMEOUT
$TRACE
$SEED
EOF
    
    # Run the simulator
    java -Dprotocol=$PROTOCOL Project < input.txt > $OUTPUT_FILE 2>&1
    
    # Extract and display key statistics
    echo "Results saved to $OUTPUT_FILE"
    echo "Key Statistics:"
    grep "Number of original packets" $OUTPUT_FILE
    grep "Number of retransmissions" $OUTPUT_FILE
    grep "Number of data packets delivered" $OUTPUT_FILE
    grep "Ratio of lost packets" $OUTPUT_FILE
    grep "Ratio of corrupted packets" $OUTPUT_FILE
    grep "Average RTT" $OUTPUT_FILE
    grep "Throughput" $OUTPUT_FILE
    grep "Goodput" $OUTPUT_FILE
    echo ""
}

# Test Case 1: Stop-and-Wait - No loss, no corruption
echo "========================================"
echo "TEST CASE 1: Stop-and-Wait Protocol"
echo "========================================"
run_test "SAW" 0.0 0.0 2 "output_saw_case1.txt"

# Test Case 2: Stop-and-Wait - With loss
echo "========================================"
echo "TEST CASE 2: Stop-and-Wait with Loss"
echo "========================================"
run_test "SAW" 0.1 0.0 2 "output_saw_case2.txt"

# Test Case 3: Stop-and-Wait - With corruption
echo "========================================"
echo "TEST CASE 3: Stop-and-Wait with Corruption"
echo "========================================"
run_test "SAW" 0.0 0.1 2 "output_saw_case3.txt"

# Test Case 4: Stop-and-Wait - With both loss and corruption
echo "========================================"
echo "TEST CASE 4: Stop-and-Wait with Loss and Corruption"
echo "========================================"
run_test "SAW" 0.1 0.1 2 "output_saw_case4.txt"

# Test Case 5: Selective Repeat - No loss, no corruption
echo "========================================"
echo "TEST CASE 5: Selective Repeat Protocol"
echo "========================================"
run_test "SR" 0.0 0.0 2 "output_sr_case1.txt"

# Test Case 6: SR - Case 2: ACK lost/corrupted with cumulative ACK recovery
echo "========================================"
echo "TEST CASE 6: SR with ACK Loss/Corruption"
echo "========================================"
run_test "SR" 0.1 0.1 3 "output_sr_case2.txt"

# Test Case 7: SR - Case 3: Data packet lost, retransmitted after RTO
echo "========================================"
echo "TEST CASE 7: SR with Data Loss and RTO"
echo "========================================"
run_test "SR" 0.2 0.0 3 "output_sr_case3.txt"

# Test Case 8: SR - Case 4: Data packet lost, retransmitted after duplicate ACK
echo "========================================"
echo "TEST CASE 8: SR with Duplicate ACK Recovery"
echo "========================================"
run_test "SR" 0.15 0.05 3 "output_sr_case4.txt"

# Test Case 9: SR - Case 5: Cumulative ACK moves window by more than 1
echo "========================================"
echo "TEST CASE 9: SR with Cumulative ACK Window Movement"
echo "========================================"
run_test "SR" 0.1 0.1 3 "output_sr_case5.txt"

# Test Case 10: GBN with SACK - No loss, no corruption
echo "========================================"
echo "TEST CASE 10: GBN with SACK Protocol"
echo "========================================"
run_test "GBN" 0.0 0.0 2 "output_gbn_case1.txt"

# Test Case 11: GBN with SACK - With loss
echo "========================================"
echo "TEST CASE 11: GBN with SACK and Loss"
echo "========================================"
run_test "GBN" 0.1 0.0 2 "output_gbn_case2.txt"

# Test Case 12: GBN with SACK - With corruption
echo "========================================"
echo "TEST CASE 12: GBN with SACK and Corruption"
echo "========================================"
run_test "GBN" 0.0 0.1 2 "output_gbn_case3.txt"

# Test Case 13: GBN with SACK - With both loss and corruption
echo "========================================"
echo "TEST CASE 13: GBN with SACK, Loss and Corruption"
echo "========================================"
run_test "GBN" 0.1 0.1 2 "output_gbn_case4.txt"

# Performance comparison test with varying loss rates
echo "========================================"
echo "PERFORMANCE COMPARISON: Varying Loss Rates"
echo "========================================"

for loss in 0.0 0.05 0.1 0.15 0.2 0.25 0.3
do
    echo "Testing with loss rate: $loss"
    run_test "SR" $loss 0.0 0 "perf_sr_loss_${loss}.txt"
    run_test "GBN" $loss 0.0 0 "perf_gbn_loss_${loss}.txt"
done

# Performance comparison test with varying corruption rates
echo "========================================"
echo "PERFORMANCE COMPARISON: Varying Corruption Rates"
echo "========================================"

for corrupt in 0.0 0.05 0.1 0.15 0.2 0.25 0.3
do
    echo "Testing with corruption rate: $corrupt"
    run_test "SR" 0.0 $corrupt 0 "perf_sr_corrupt_${corrupt}.txt"
    run_test "GBN" 0.0 $corrupt 0 "perf_gbn_corrupt_${corrupt}.txt"
done

echo "========================================"
echo "All tests completed!"
echo "========================================"
echo ""
echo "Output files generated:"
echo "  - output_saw_case*.txt: Stop-and-Wait test cases"
echo "  - output_sr_case*.txt: Selective Repeat test cases"
echo "  - output_gbn_case*.txt: GBN with SACK test cases"
echo "  - perf_*.txt: Performance comparison files"
echo ""
echo "To view detailed traces, examine the output files."
echo "Look for the following patterns in SR traces:"
echo "  - 'duplicate ACK' messages for Case 4"
echo "  - 'timeout' messages for Case 3"
echo "  - 'cumulative ACK moved window' messages for Cases 2 and 5"