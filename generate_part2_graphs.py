import matplotlib.pyplot as plt
import numpy as np

# Data for Part 2 SR vs GBN+SACK Comparison
# You need to run BOTH protocols and collect these values
corruption_rates = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5]
loss_rates = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5]

# === UNDER CORRUPTION ===
# Throughput data (from your senior's report - replace with YOUR data)
sr_throughput_corr = [6132.90, 5205.54, 4600.31, 3819.48, 3290.17, 2713.20]
gbn_throughput_corr = [7991.35, 5810.09, 4472.73, 3872.96, 3109.36, 2821.34]

# Goodput data
sr_goodput_corr = [3902.75, 1991.23, 973.14, 503.51, 230.83, 113.41]
gbn_goodput_corr = [3902.75, 1981.47, 971.08, 499.58, 244.98, 114.85]

# Average Packet Delay
sr_delay_corr = [10.76, 17.32, 28.27, 42.79, 70.05, 118.88]
gbn_delay_corr = [10.76, 17.36, 27.68, 41.11, 61.03, 87.12]

# === UNDER LOSS ===
# Throughput data
sr_throughput_loss = [6132.90, 5152.43, 4746.44, 3936.33, 3330.84, 2626.69]
gbn_throughput_loss = [7991.35, 5677.39, 4829.07, 3964.70, 3145.71, 2814.61]

# Goodput data
sr_goodput_loss = [3902.75, 1887.03, 922.39, 480.91, 231.82, 112.80]
gbn_goodput_loss = [3902.75, 1880.45, 936.64, 503.26, 244.37, 105.70]

# Average Packet Delay
sr_delay_loss = [10.76, 18.20, 28.65, 42.56, 67.88, 124.32]
gbn_delay_loss = [10.76, 18.16, 28.41, 41.40, 61.60, 87.50]

print("Generating Part 2 comparison graphs...")

# 1. Throughput Comparison (Under Corruption)
plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, sr_throughput_corr, 'o-', label='Selective Repeat Throughput', 
         color='blue', linewidth=2, markersize=8)
plt.plot(corruption_rates, gbn_throughput_corr, 'D-', label='Go Back N Throughput', 
         color='green', linewidth=2, markersize=8)

# Add value labels
for i in range(len(corruption_rates)):
    plt.annotate(f'{sr_throughput_corr[i]:.2f}', 
                (corruption_rates[i], sr_throughput_corr[i]),
                textcoords='offset points', xytext=(-15,5), fontsize=8)
    plt.annotate(f'{gbn_throughput_corr[i]:.2f}', 
                (corruption_rates[i], gbn_throughput_corr[i]),
                textcoords='offset points', xytext=(15,5), fontsize=8)

plt.xlabel('Corruption Probability')
plt.ylabel('Throughput (Bytes/Second)')
plt.title('SR vs. GBN+SACK (Under Corruption)\nThroughput')
plt.legend(loc='upper right')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(2000, 8500)
plt.savefig('part2_throughput_corruption.png', dpi=100)
plt.close()
print("✓ Saved: part2_throughput_corruption.png")

# 2. Goodput Comparison (Under Corruption)
plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, sr_goodput_corr, 'o-', label='Selective Repeat Goodput', 
         color='blue', linewidth=2, markersize=8)
plt.plot(corruption_rates, gbn_goodput_corr, 'D-', label='Go Back N Goodput', 
         color='green', linewidth=2, markersize=8)

for i in range(len(corruption_rates)):
    plt.annotate(f'{sr_goodput_corr[i]:.2f}', 
                (corruption_rates[i], sr_goodput_corr[i]),
                textcoords='offset points', xytext=(-15,5), fontsize=8)
    plt.annotate(f'{gbn_goodput_corr[i]:.2f}', 
                (corruption_rates[i], gbn_goodput_corr[i]),
                textcoords='offset points', xytext=(15,-10), fontsize=8)

plt.xlabel('Corruption Probability')
plt.ylabel('Goodput (Bytes/Second)')
plt.title('SR vs. GBN+SACK (Under Corruption)\nGoodput')
plt.legend(loc='upper right')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(0, 4500)
plt.savefig('part2_goodput_corruption.png', dpi=100)
plt.close()
print("✓ Saved: part2_goodput_corruption.png")

# 3. Average Packet Delay (Under Corruption)
plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, sr_delay_corr, 'o-', label='Selective Repeat Average Packet Delay', 
         color='blue', linewidth=2, markersize=8)
plt.plot(corruption_rates, gbn_delay_corr, 'D-', label='Go Back N Average Packet Delay', 
         color='green', linewidth=2, markersize=8)

for i in range(len(corruption_rates)):
    plt.annotate(f'{sr_delay_corr[i]:.2f}', 
                (corruption_rates[i], sr_delay_corr[i]),
                textcoords='offset points', xytext=(-15,5), fontsize=8)
    plt.annotate(f'{gbn_delay_corr[i]:.2f}', 
                (corruption_rates[i], gbn_delay_corr[i]),
                textcoords='offset points', xytext=(15,5), fontsize=8)

plt.xlabel('Corruption Probability')
plt.ylabel('Average Packet Delay (s)')
plt.title('SR vs. GBN+SACK (Under Corruption)\nAverage Packet Delay')
plt.legend(loc='upper left')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(0, 130)
plt.savefig('part2_delay_corruption.png', dpi=100)
plt.close()
print("✓ Saved: part2_delay_corruption.png")

# 4. Throughput Comparison (Under Loss)
plt.figure(figsize=(10, 6))
plt.plot(loss_rates, sr_throughput_loss, 'o-', label='Selective Repeat Throughput', 
         color='blue', linewidth=2, markersize=8)
plt.plot(loss_rates, gbn_throughput_loss, 'D-', label='Go Back N Throughput', 
         color='green', linewidth=2, markersize=8)

for i in range(len(loss_rates)):
    plt.annotate(f'{sr_throughput_loss[i]:.2f}', 
                (loss_rates[i], sr_throughput_loss[i]),
                textcoords='offset points', xytext=(-15,5), fontsize=8)
    plt.annotate(f'{gbn_throughput_loss[i]:.2f}', 
                (loss_rates[i], gbn_throughput_loss[i]),
                textcoords='offset points', xytext=(15,5), fontsize=8)

plt.xlabel('Loss Probability')
plt.ylabel('Throughput (Bytes/Second)')
plt.title('SR vs. GBN+SACK (Under Loss)\nThroughput')
plt.legend(loc='upper right')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(2000, 8500)
plt.savefig('part2_throughput_loss.png', dpi=100)
plt.close()
print("✓ Saved: part2_throughput_loss.png")

# 5. Goodput Comparison (Under Loss)
plt.figure(figsize=(10, 6))
plt.plot(loss_rates, sr_goodput_loss, 'o-', label='Selective Repeat Goodput', 
         color='blue', linewidth=2, markersize=8)
plt.plot(loss_rates, gbn_goodput_loss, 'D-', label='Go Back N Goodput', 
         color='green', linewidth=2, markersize=8)

for i in range(len(loss_rates)):
    plt.annotate(f'{sr_goodput_loss[i]:.2f}', 
                (loss_rates[i], sr_goodput_loss[i]),
                textcoords='offset points', xytext=(-15,5), fontsize=8)
    plt.annotate(f'{gbn_goodput_loss[i]:.2f}', 
                (loss_rates[i], gbn_goodput_loss[i]),
                textcoords='offset points', xytext=(15,-10), fontsize=8)

plt.xlabel('Loss Probability')
plt.ylabel('Goodput (Bytes/Second)')
plt.title('SR vs. GBN+SACK (Under Loss)\nGoodput')
plt.legend(loc='upper right')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(0, 4500)
plt.savefig('part2_goodput_loss.png', dpi=100)
plt.close()
print("✓ Saved: part2_goodput_loss.png")

# 6. Average Packet Delay (Under Loss)
plt.figure(figsize=(10, 6))
plt.plot(loss_rates, sr_delay_loss, 'o-', label='Selective Repeat Average Packet Delay', 
         color='blue', linewidth=2, markersize=8)
plt.plot(loss_rates, gbn_delay_loss, 'D-', label='Go Back N Average Packet Delay', 
         color='green', linewidth=2, markersize=8)

for i in range(len(loss_rates)):
    plt.annotate(f'{sr_delay_loss[i]:.2f}', 
                (loss_rates[i], sr_delay_loss[i]),
                textcoords='offset points', xytext=(-15,5), fontsize=8)
    plt.annotate(f'{gbn_delay_loss[i]:.2f}', 
                (loss_rates[i], gbn_delay_loss[i]),
                textcoords='offset points', xytext=(15,5), fontsize=8)

plt.xlabel('Loss Probability')
plt.ylabel('Average Packet Delay (s)')
plt.title('SR vs. GBN+SACK (Under Loss)\nAverage Packet Delay')
plt.legend(loc='upper left')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(0, 140)
plt.savefig('part2_delay_loss.png', dpi=100)
plt.close()
print("✓ Saved: part2_delay_loss.png")

print("\n✅ All Part 2 graphs generated successfully!")
print("\nFiles created:")
print("  - part2_throughput_corruption.png")
print("  - part2_goodput_corruption.png")
print("  - part2_delay_corruption.png")
print("  - part2_throughput_loss.png")
print("  - part2_goodput_loss.png")
print("  - part2_delay_loss.png")