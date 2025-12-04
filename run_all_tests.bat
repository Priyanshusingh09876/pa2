@echo off
echo ========================================
echo Testing All Three Protocols
echo ========================================
echo.

REM Compile the code
echo Compiling Java files...
javac *.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo STOP-AND-WAIT TESTS (Window Size = 1)
echo ========================================
echo.

REM SAW Test 1: No loss, no corruption
echo 20 > temp.txt
echo 0.0 >> temp.txt
echo 0.0 >> temp.txt
echo 100.0 >> temp.txt
echo 1 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running SAW Test 1: No loss, no corruption...
java Project < temp.txt > saw_test1_clean.txt

REM SAW Test 2: With loss
echo 20 > temp.txt
echo 0.1 >> temp.txt
echo 0.0 >> temp.txt
echo 100.0 >> temp.txt
echo 1 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running SAW Test 2: With loss...
java Project < temp.txt > saw_test2_loss.txt

REM SAW Test 3: With corruption
echo 20 > temp.txt
echo 0.0 >> temp.txt
echo 0.1 >> temp.txt
echo 100.0 >> temp.txt
echo 1 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running SAW Test 3: With corruption...
java Project < temp.txt > saw_test3_corruption.txt

REM SAW Test 4: With both
echo 20 > temp.txt
echo 0.1 >> temp.txt
echo 0.1 >> temp.txt
echo 100.0 >> temp.txt
echo 1 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running SAW Test 4: With both loss and corruption...
java Project < temp.txt > saw_test4_both.txt

echo.
echo ========================================
echo SELECTIVE REPEAT TESTS (Window Size = 8)
echo ========================================
echo.

REM SR Case 1: No loss, no corruption
echo 1000 > temp.txt
echo 0.0 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 3 >> temp.txt
echo 1234 >> temp.txt
echo Running SR Case 1: No loss, no corruption...
java Project < temp.txt > sr_case1_clean.txt

REM SR Case 2: ACK loss/corruption
echo 1000 > temp.txt
echo 0.1 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 3 >> temp.txt
echo 1234 >> temp.txt
echo Running SR Case 2: ACK loss/corruption...
java Project < temp.txt > sr_case2_ack_loss.txt

REM SR Case 3: Data loss (RTO)
echo 1000 > temp.txt
echo 0.2 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 3 >> temp.txt
echo 1234 >> temp.txt
echo Running SR Case 3: Data loss with RTO...
java Project < temp.txt > sr_case3_rto.txt

REM SR Case 4: Duplicate ACK
echo 1000 > temp.txt
echo 0.15 >> temp.txt
echo 0.05 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 3 >> temp.txt
echo 1234 >> temp.txt
echo Running SR Case 4: Duplicate ACK...
java Project < temp.txt > sr_case4_dup_ack.txt

REM SR Case 5: Cumulative ACK
echo 1000 > temp.txt
echo 0.1 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 3 >> temp.txt
echo 1234 >> temp.txt
echo Running SR Case 5: Cumulative ACK...
java Project < temp.txt > sr_case5_cumulative.txt

echo.
echo ========================================
echo GO-BACK-N WITH SACK TESTS (Window Size = 16)
echo ========================================
echo.

REM GBN Test 1: No loss, no corruption (should match SR)
echo 1000 > temp.txt
echo 0.0 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 16 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running GBN Test 1: No loss, no corruption...
java Project < temp.txt > gbn_test1_clean.txt

REM GBN Test 2: With loss
echo 1000 > temp.txt
echo 0.1 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 16 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running GBN Test 2: With loss...
java Project < temp.txt > gbn_test2_loss.txt

REM GBN Test 3: With corruption
echo 1000 > temp.txt
echo 0.0 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 16 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running GBN Test 3: With corruption...
java Project < temp.txt > gbn_test3_corruption.txt

REM GBN Test 4: With both
echo 1000 > temp.txt
echo 0.1 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 16 >> temp.txt
echo 30.0 >> temp.txt
echo 2 >> temp.txt
echo 1234 >> temp.txt
echo Running GBN Test 4: With both loss and corruption...
java Project < temp.txt > gbn_test4_both.txt

echo.
echo ========================================
echo PERFORMANCE COMPARISON: SR vs GBN
echo ========================================
echo.

REM SR Performance with 10% loss
echo 100 > temp.txt
echo 0.1 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
echo Running SR with 10%% loss...
java Project < temp.txt > perf_sr_loss_10.txt

REM GBN Performance with 10% loss
echo 100 > temp.txt
echo 0.1 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 16 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
echo Running GBN with 10%% loss...
java Project < temp.txt > perf_gbn_loss_10.txt

REM SR Performance with 10% corruption
echo 100 > temp.txt
echo 0.0 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
echo Running SR with 10%% corruption...
java Project < temp.txt > perf_sr_corrupt_10.txt

REM GBN Performance with 10% corruption
echo 100 > temp.txt
echo 0.0 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 16 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
echo Running GBN with 10%% corruption...
java Project < temp.txt > perf_gbn_corrupt_10.txt

del temp.txt

echo.
echo ========================================
echo EXTRACTING STATISTICS
echo ========================================
echo.

echo Creating statistics summary...
echo Protocol Performance Summary > statistics_summary.txt
echo ============================ >> statistics_summary.txt
echo. >> statistics_summary.txt
echo Stop-and-Wait Statistics: >> statistics_summary.txt
findstr "Number of retransmissions" saw_test*.txt >> statistics_summary.txt
echo. >> statistics_summary.txt
echo Selective Repeat Statistics: >> statistics_summary.txt
findstr "Number of retransmissions" sr_case*.txt >> statistics_summary.txt
echo. >> statistics_summary.txt
echo GBN with SACK Statistics: >> statistics_summary.txt
findstr "Number of retransmissions" gbn_test*.txt >> statistics_summary.txt
echo. >> statistics_summary.txt
echo Performance Comparison (10%% loss): >> statistics_summary.txt
findstr "Average communication time" perf_sr_loss_10.txt perf_gbn_loss_10.txt >> statistics_summary.txt
findstr "Throughput" perf_sr_loss_10.txt perf_gbn_loss_10.txt >> statistics_summary.txt
findstr "Goodput" perf_sr_loss_10.txt perf_gbn_loss_10.txt >> statistics_summary.txt

echo.
echo ========================================
echo ALL TESTS COMPLETED!
echo ========================================
echo.
echo Check the following files:
echo - saw_test*.txt (4 files) - Stop-and-Wait tests
echo - sr_case*.txt (5 files) - Selective Repeat tests
echo - gbn_test*.txt (4 files) - GBN with SACK tests
echo - perf_*.txt - Performance comparison files
echo - statistics_summary.txt - Summary of all statistics
echo.
pause