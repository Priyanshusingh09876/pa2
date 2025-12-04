@echo off
echo Running performance tests...

echo 100 > temp.txt
echo 0.0 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
java Project < temp.txt > perf_loss_0.txt

echo 100 > temp.txt
echo 0.1 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
java Project < temp.txt > perf_loss_10.txt

echo 100 > temp.txt
echo 0.2 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
java Project < temp.txt > perf_loss_20.txt

echo 100 > temp.txt
echo 0.3 >> temp.txt
echo 0.0 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
java Project < temp.txt > perf_loss_30.txt

echo 100 > temp.txt
echo 0.0 >> temp.txt
echo 0.1 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
java Project < temp.txt > perf_corrupt_10.txt

echo 100 > temp.txt
echo 0.0 >> temp.txt
echo 0.2 >> temp.txt
echo 200.0 >> temp.txt
echo 8 >> temp.txt
echo 30.0 >> temp.txt
echo 0 >> temp.txt
echo 1234 >> temp.txt
java Project < temp.txt > perf_corrupt_20.txt

del temp.txt
echo Done!