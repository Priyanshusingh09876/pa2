import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')  # This prevents the popup windows

# Data from your senior's report (replace with your actual data)
loss_rates = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5]
corruption_rates = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5]

# Replace these with YOUR actual values from running your Java program
retrans_loss = [0, 277, 699, 1027, 1631, 2665]
retrans_corruption = [0, 240, 640, 999, 1677, 2624]
rtt_loss = [10.762, 11.111, 11.108, 11.098, 11.305, 13.125]
rtt_corruption = [10.762, 11.111, 11.108, 11.098, 11.305, 13.125]
comm_time_loss = [10.762, 18.203, 28.653, 42.362, 67.89, 124.322]
comm_time_corruption = [10.762, 17.317, 28.269, 42.792, 70.05, 118.875]

print("Generating all graphs...")

# Graph 1: Retransmissions vs Loss
plt.figure(figsize=(10, 6))
plt.plot(loss_rates, retrans_loss, 'o-', color='#FFA500', linewidth=2.5, markersize=10)
for i, txt in enumerate(retrans_loss):
    plt.annotate(str(txt), (loss_rates[i], retrans_loss[i]),
                textcoords="offset points", xytext=(0,10), ha='center', fontweight='bold')
plt.xlabel('Loss Percentage')
plt.ylabel('Number of Retransmissions')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(-100, 3000)
plt.savefig('1_retrans_vs_loss.png', dpi=100)
plt.close()
print("✓ Saved: 1_retrans_vs_loss.png")

# Graph 2: Retransmissions vs Corruption
plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, retrans_corruption, 'o-', color='#FFA500', linewidth=2.5, markersize=10)
for i, txt in enumerate(retrans_corruption):
    plt.annotate(str(txt), (corruption_rates[i], retrans_corruption[i]),
                textcoords="offset points", xytext=(0,10), ha='center', fontweight='bold')
plt.xlabel('Corruption Percentage')
plt.ylabel('Number of Retransmissions')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(-100, 3000)
plt.savefig('2_retrans_vs_corruption.png', dpi=100)
plt.close()
print("✓ Saved: 2_retrans_vs_corruption.png")

# Graph 3: Average RTT vs Loss
plt.figure(figsize=(10, 6))
plt.plot(loss_rates, rtt_loss, 'o-', color='#FFA500', linewidth=2.5, markersize=10)
for i, txt in enumerate(rtt_loss):
    plt.annotate(f'{txt:.3f}', (loss_rates[i], rtt_loss[i]),
                textcoords="offset points", xytext=(0,10), ha='center', fontweight='bold')
plt.xlabel('Loss Percentage')
plt.ylabel('Average Round Trip Time')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(10, 14)
plt.savefig('3_rtt_vs_loss.png', dpi=100)
plt.close()
print("✓ Saved: 3_rtt_vs_loss.png")

# Graph 4: Average RTT vs Corruption
plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, rtt_corruption, 'o-', color='#FFA500', linewidth=2.5, markersize=10)
for i, txt in enumerate(rtt_corruption):
    plt.annotate(f'{txt:.3f}', (corruption_rates[i], rtt_corruption[i]),
                textcoords="offset points", xytext=(0,10), ha='center', fontweight='bold')
plt.xlabel('Corruption Percentage')
plt.ylabel('Average Round Trip Time')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(10, 14)
plt.savefig('4_rtt_vs_corruption.png', dpi=100)
plt.close()
print("✓ Saved: 4_rtt_vs_corruption.png")

# Graph 5: Communication Time vs Loss
plt.figure(figsize=(10, 6))
plt.plot(loss_rates, comm_time_loss, 'o-', color='#FFA500', linewidth=2.5, markersize=10)
for i, txt in enumerate(comm_time_loss):
    plt.annotate(f'{txt:.2f}', (loss_rates[i], comm_time_loss[i]),
                textcoords="offset points", xytext=(0,10), ha='center', fontweight='bold')
plt.xlabel('Loss Percentage')
plt.ylabel('Average Communication Time')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(0, 140)
plt.savefig('5_commtime_vs_loss.png', dpi=100)
plt.close()
print("✓ Saved: 5_commtime_vs_loss.png")

# Graph 6: Communication Time vs Corruption
plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, comm_time_corruption, 'o-', color='#FFA500', linewidth=2.5, markersize=10)
for i, txt in enumerate(comm_time_corruption):
    plt.annotate(f'{txt:.2f}', (corruption_rates[i], comm_time_corruption[i]),
                textcoords="offset points", xytext=(0,10), ha='center', fontweight='bold')
plt.xlabel('Corruption Percentage')
plt.ylabel('Average Communication Time')
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.ylim(0, 140)
plt.savefig('6_commtime_vs_corruption.png', dpi=100)
plt.close()
print("✓ Saved: 6_commtime_vs_corruption.png")

# PART 2: SR vs GBN Comparison Graphs
print("\nGenerating Part 2 comparison graphs...")

# Throughput comparison
sr_throughput = [6132.90, 5205.54, 4600.31, 3819.48, 3290.17, 2713.20]
gbn_throughput = [7991.35, 5810.09, 4472.73, 3872.96, 3109.36, 2821.34]

plt.figure(figsize=(10, 6))
plt.plot(corruption_rates, sr_throughput, 'o-', label='Selective Repeat', color='blue', linewidth=2, markersize=8)
plt.plot(corruption_rates, gbn_throughput, 'o-', label='Go Back N', color='green', linewidth=2, markersize=8)
for i in range(len(corruption_rates)):
    plt.annotate(f'{sr_throughput[i]:.2f}', (corruption_rates[i], sr_throughput[i]),
                textcoords="offset points", xytext=(-15,5), fontsize=9)
    plt.annotate(f'{gbn_throughput[i]:.2f}', (corruption_rates[i], gbn_throughput[i]),
                textcoords="offset points", xytext=(15,5), fontsize=9)
plt.xlabel('Corruption Probability')
plt.ylabel('Throughput (Bytes/Second)')
plt.title('SR vs. GBN+SACK Throughput Comparison')
plt.legend()
plt.grid(True, alpha=0.3)
plt.xlim(-0.05, 0.55)
plt.savefig('7_sr_vs_gbn_throughput.png', dpi=100)
plt.close()
print("✓ Saved: 7_sr_vs_gbn_throughput.png")

print("\n✅ All graphs generated successfully!")
print("Check your folder for the following files:")
print("  1_retrans_vs_loss.png")
print("  2_retrans_vs_corruption.png")
print("  3_rtt_vs_loss.png")
print("  4_rtt_vs_corruption.png")
print("  5_commtime_vs_loss.png")
print("  6_commtime_vs_corruption.png")
print("  7_sr_vs_gbn_throughput.png")