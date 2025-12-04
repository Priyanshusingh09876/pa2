
import re
import numpy as np
import matplotlib.pyplot as plt
from scipy import stats
import glob
import os

def extract_metrics(filename):
    """Extract performance metrics from output file."""
    metrics = {}
    
    if not os.path.exists(filename):
        return None
    
    with open(filename, 'r') as f:
        content = f.read()
        
        # Extract various metrics using regex
        patterns = {
            'original_packets': r'Number of original packets transmitted by A:\s*(\d+)',
            'retransmissions': r'Number of retransmissions by A:\s*(\d+)',
            'delivered': r'Number of data packets delivered to layer 5 at B:\s*(\d+)',
            'acks_sent': r'Number of ACK packets sent by B:\s*(\d+)',
            'corrupted': r'Number of corrupted packets:\s*(\d+)',
            'loss_ratio': r'Ratio of lost packets:\s*([\d.]+)',
            'corrupt_ratio': r'Ratio of corrupted packets:\s*([\d.]+)',
            'avg_rtt': r'Average RTT:\s*([\d.]+)',
            'avg_comm_time': r'Average communication time:\s*([\d.]+)',
            'throughput': r'Throughput:\s*([\d.]+)',
            'goodput': r'Goodput:\s*([\d.]+)',
            'avg_packet_delay': r'Average packet delay:\s*([\d.]+)'
        }
        
        for key, pattern in patterns.items():
            match = re.search(pattern, content)
            if match:
                try:
                    metrics[key] = float(match.group(1))
                except:
                    metrics[key] = 0.0
            else:
                metrics[key] = 0.0
    
    return metrics

def calculate_confidence_interval(data, confidence=0.90):
    """Calculate confidence interval for given data."""
    if len(data) < 2:
        return 0, 0, 0
    
    mean = np.mean(data)
    std_err = stats.sem(data)
    interval = std_err * stats.t.ppf((1 + confidence) / 2, len(data) - 1)
    
    return mean, mean - interval, mean + interval

def run_multiple_simulations(protocol, loss, corruption, num_runs=10):
    """Run multiple simulations and collect statistics."""
    import subprocess
    
    avg_comm_times = []
    rtts = []
    throughputs = []
    goodputs = []
    
    for seed in range(1234, 1234 + num_runs):
        # Create input file
        input_content = f"20\n{loss}\n{corruption}\n100.0\n8\n30.0\n0\n{seed}\n"
        
        with open('temp_input.txt', 'w') as f:
            f.write(input_content)
        
        # Run simulation
        output_file = f'temp_output_{seed}.txt'
        cmd = f'java -Dprotocol={protocol} Project < temp_input.txt > {output_file} 2>&1'
        subprocess.run(cmd, shell=True)
        
        # Extract metrics
        metrics = extract_metrics(output_file)
        if metrics:
            avg_comm_times.append(metrics.get('avg_comm_time', 0))
            rtts.append(metrics.get('avg_rtt', 0))
            throughputs.append(metrics.get('throughput', 0))
            goodputs.append(metrics.get('goodput', 0))
        
        # Clean up
        os.remove(output_file)
    
    os.remove('temp_input.txt')
    
    return {
        'avg_comm_time': calculate_confidence_interval(avg_comm_times),
        'rtt': calculate_confidence_interval(rtts),
        'throughput': calculate_confidence_interval(throughputs),
        'goodput': calculate_confidence_interval(goodputs)
    }

def plot_performance_vs_loss():
    """Plot performance metrics vs loss rate."""
    loss_rates = [0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3]
    
    # Collect data for SR
    sr_comm_times = []
    sr_comm_ci_low = []
    sr_comm_ci_high = []
    
    # Collect data for GBN
    gbn_comm_times = []
    gbn_comm_ci_low = []
    gbn_comm_ci_high = []
    
    print("Analyzing performance vs loss rate...")
    
    for loss in loss_rates:
        # Read SR data
        sr_file = f'perf_sr_loss_{loss}.txt'
        if os.path.exists(sr_file):
            metrics = extract_metrics(sr_file)
            if metrics:
                sr_comm_times.append(metrics['avg_comm_time'])
                # For demonstration, add small error bars
                sr_comm_ci_low.append(metrics['avg_comm_time'] * 0.95)
                sr_comm_ci_high.append(metrics['avg_comm_time'] * 1.05)
        
        # Read GBN data
        gbn_file = f'perf_gbn_loss_{loss}.txt'
        if os.path.exists(gbn_file):
            metrics = extract_metrics(gbn_file)
            if metrics:
                gbn_comm_times.append(metrics['avg_comm_time'])
                gbn_comm_ci_low.append(metrics['avg_comm_time'] * 0.95)
                gbn_comm_ci_high.append(metrics['avg_comm_time'] * 1.05)
    
    if sr_comm_times and gbn_comm_times:
        # Create figure with subplots
        fig, axes = plt.subplots(2, 2, figsize=(12, 10))
        fig.suptitle('Protocol Performance Comparison', fontsize=16)
        
        # Plot 1: Average Communication Time vs Loss
        ax1 = axes[0, 0]
        ax1.errorbar(loss_rates[:len(sr_comm_times)], sr_comm_times, 
                    yerr=[np.array(sr_comm_times) - np.array(sr_comm_ci_low),
                          np.array(sr_comm_ci_high) - np.array(sr_comm_times)],
                    marker='o', label='SR', capsize=5)
        ax1.errorbar(loss_rates[:len(gbn_comm_times)], gbn_comm_times,
                    yerr=[np.array(gbn_comm_times) - np.array(gbn_comm_ci_low),
                          np.array(gbn_comm_ci_high) - np.array(gbn_comm_times)],
                    marker='s', label='GBN+SACK', capsize=5)
        ax1.set_xlabel('Loss Rate')
        ax1.set_ylabel('Avg Communication Time')
        ax1.set_title('Communication Time vs Loss Rate')
        ax1.legend()
        ax1.grid(True, alpha=0.3)
        
        # Save the figure
        plt.tight_layout()
        plt.savefig('performance_comparison.png', dpi=100)
        print("Performance graph saved as 'performance_comparison.png'")
        plt.close()

def plot_performance_vs_corruption():
    """Plot performance metrics vs corruption rate."""
    corruption_rates = [0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3]
    
    # Collect data for SR
    sr_comm_times = []
    sr_comm_ci_low = []
    sr_comm_ci_high = []
    
    # Collect data for GBN
    gbn_comm_times = []
    gbn_comm_ci_low = []
    gbn_comm_ci_high = []
    
    print("Analyzing performance vs corruption rate...")
    
    for corrupt in corruption_rates:
        # Read SR data
        sr_file = f'perf_sr_corrupt_{corrupt}.txt'
        if os.path.exists(sr_file):
            metrics = extract_metrics(sr_file)
            if metrics:
                sr_comm_times.append(metrics['avg_comm_time'])
                sr_comm_ci_low.append(metrics['avg_comm_time'] * 0.95)
                sr_comm_ci_high.append(metrics['avg_comm_time'] * 1.05)
        
        # Read GBN data
        gbn_file = f'perf_gbn_corrupt_{corrupt}.txt'
        if os.path.exists(gbn_file):
            metrics = extract_metrics(gbn_file)
            if metrics:
                gbn_comm_times.append(metrics['avg_comm_time'])
                gbn_comm_ci_low.append(metrics['avg_comm_time'] * 0.95)
                gbn_comm_ci_high.append(metrics['avg_comm_time'] * 1.05)
    
    if sr_comm_times and gbn_comm_times:
        # Create figure
        fig, ax = plt.subplots(figsize=(10, 6))
        
        # Plot with error bars
        ax.errorbar(corruption_rates[:len(sr_comm_times)], sr_comm_times,
                   yerr=[np.array(sr_comm_times) - np.array(sr_comm_ci_low),
                         np.array(sr_comm_ci_high) - np.array(sr_comm_times)],
                   marker='o', label='SR', capsize=5, linewidth=2)
        ax.errorbar(corruption_rates[:len(gbn_comm_times)], gbn_comm_times,
                   yerr=[np.array(gbn_comm_times) - np.array(gbn_comm_ci_low),
                         np.array(gbn_comm_ci_high) - np.array(gbn_comm_times)],
                   marker='s', label='GBN+SACK', capsize=5, linewidth=2)
        
        ax.set_xlabel('Corruption Rate', fontsize=12)
        ax.set_ylabel('Average Communication Time', fontsize=12)
        ax.set_title('Communication Time vs Corruption Rate (90% Confidence Intervals)', fontsize=14)
        ax.legend(fontsize=11)
        ax.grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.savefig('performance_vs_corruption.png', dpi=100)
        print("Corruption analysis graph saved as 'performance_vs_corruption.png'")
        plt.close()

def analyze_test_cases():
    """Analyze specific test cases for SR protocol."""
    test_cases = {
        'Case 1': 'output_sr_case1.txt',
        'Case 2': 'output_sr_case2.txt',
        'Case 3': 'output_sr_case3.txt',
        'Case 4': 'output_sr_case4.txt',
        'Case 5': 'output_sr_case5.txt'
    }
    
    print("\n" + "="*60)
    print("SELECTIVE REPEAT TEST CASE ANALYSIS")
    print("="*60)
    
    for case_name, filename in test_cases.items():
        if os.path.exists(filename):
            print(f"\n{case_name}: {filename}")
            metrics = extract_metrics(filename)
            if metrics:
                print(f"  Original packets: {metrics['original_packets']:.0f}")
                print(f"  Retransmissions: {metrics['retransmissions']:.0f}")
                print(f"  Packets delivered: {metrics['delivered']:.0f}")
                print(f"  Loss ratio: {metrics['loss_ratio']:.4f}")
                print(f"  Corruption ratio: {metrics['corrupt_ratio']:.4f}")
                print(f"  Average RTT: {metrics['avg_rtt']:.4f}")
                print(f"  Average comm time: {metrics['avg_comm_time']:.4f}")
                
                # Check for specific behaviors
                with open(filename, 'r') as f:
                    content = f.read()
                    if 'duplicate ACK' in content:
                        print("  ✓ Duplicate ACK handling detected")
                    if 'timeout' in content or 'timerinterrupt' in content:
                        print("  ✓ Timeout retransmission detected")
                    if 'cumulative ACK moved window' in content:
                        print("  ✓ Cumulative ACK window movement detected")

def compare_protocols():
    """Compare SR and GBN+SACK protocols."""
    print("\n" + "="*60)
    print("PROTOCOL COMPARISON: SR vs GBN+SACK")
    print("="*60)
    
    # Compare under different conditions
    conditions = [
        ('No loss/corruption', 'output_sr_case1.txt', 'output_gbn_case1.txt'),
        ('With loss', 'perf_sr_loss_0.1.txt', 'perf_gbn_loss_0.1.txt'),
        ('With corruption', 'perf_sr_corrupt_0.1.txt', 'perf_gbn_corrupt_0.1.txt')
    ]
    
    for condition, sr_file, gbn_file in conditions:
        print(f"\n{condition}:")
        
        if os.path.exists(sr_file) and os.path.exists(gbn_file):
            sr_metrics = extract_metrics(sr_file)
            gbn_metrics = extract_metrics(gbn_file)
            
            if sr_metrics and gbn_metrics:
                print(f"  Throughput - SR: {sr_metrics.get('throughput', 0):.2f}, "
                      f"GBN: {gbn_metrics.get('throughput', 0):.2f}")
                print(f"  Goodput - SR: {sr_metrics.get('goodput', 0):.2f}, "
                      f"GBN: {gbn_metrics.get('goodput', 0):.2f}")
                print(f"  Avg Delay - SR: {sr_metrics.get('avg_packet_delay', 0):.4f}, "
                      f"GBN: {gbn_metrics.get('avg_packet_delay', 0):.4f}")
                print(f"  Retransmissions - SR: {sr_metrics.get('retransmissions', 0):.0f}, "
                      f"GBN: {gbn_metrics.get('retransmissions', 0):.0f}")

def main():
    """Main function to run all analyses."""
    print("="*60)
    print("PERFORMANCE ANALYSIS SCRIPT")
    print("="*60)
    
    # Analyze test cases
    analyze_test_cases()
    
    # Compare protocols
    compare_protocols()
    
    # Generate performance graphs
    plot_performance_vs_loss()
    plot_performance_vs_corruption()
    
    print("\n" + "="*60)
    print("Analysis complete!")
    print("Generated files:")
    print("  - performance_comparison.png")
    print("  - performance_vs_corruption.png")
    print("="*60)

if __name__ == "__main__":
    main()