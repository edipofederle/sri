#!/bin/bash

# Sri Benchmark Runner with Performance Statistics
# Usage: ./benchmark/run_benchmarks.sh [mode] [output_file]
# Modes: lein, native, jar, all

set -e

# Configuration
BENCHMARK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$BENCHMARK_DIR")"
EXECUTION_MODE="${1:-lein}"  # lein, native, jar, or all
OUTPUT_FILE="${2:-benchmark_results.txt}"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Execution modes
NATIVE_BINARY="$PROJECT_ROOT/target/sri"
JAR_FILE="$PROJECT_ROOT/target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Benchmark files
BENCHMARKS=(
    "method_dispatch_benchmark.rb"
    "arithmetic_benchmark.rb"
    "string_benchmark.rb"
    "array_benchmark.rb"
    "method_call_benchmark.rb"
    "class_benchmark.rb"
    "control_flow_benchmark.rb"
)

# Function to get the appropriate command for execution mode
get_run_command() {
    local benchmark_file="$1"
    local mode="$2"

    case "$mode" in
        "lein")
            echo "lein run benchmark/$benchmark_file"
            ;;
        "native")
            echo "$NATIVE_BINARY benchmark/$benchmark_file"
            ;;
        "jar")
            echo "java -jar $JAR_FILE benchmark/$benchmark_file"
            ;;
        *)
            echo "lein run benchmark/$benchmark_file"
            ;;
    esac
}

# Function to run a single benchmark and collect stats
run_benchmark() {
    local benchmark_file="$1"
    local mode="$2"
    local benchmark_name=$(basename "$benchmark_file" .rb)

    echo -e "${BLUE}Running $benchmark_name ($mode mode)...${NC}"

    # Get the appropriate command
    local run_command=$(get_run_command "$benchmark_file" "$mode")

    # Run benchmark multiple times for better statistics
    local total_time=0
    local max_time=0
    local min_time=999999
    local runs=3

    for ((i=1; i<=runs; i++)); do
        echo "  Run $i/$runs..."

        # Time the execution
        local start_time=$(date +%s.%N)

        # Run the benchmark (capture output and errors)
        if eval "$run_command" > "/tmp/sri_benchmark_output_$i.txt" 2>&1; then
            local end_time=$(date +%s.%N)
            local elapsed=$(echo "$end_time - $start_time" | bc -l)

            # Update statistics
            total_time=$(echo "$total_time + $elapsed" | bc -l)

            if (( $(echo "$elapsed > $max_time" | bc -l) )); then
                max_time=$elapsed
            fi

            if (( $(echo "$elapsed < $min_time" | bc -l) )); then
                min_time=$elapsed
            fi

            echo "    Time: ${elapsed}s"
        else
            echo -e "    ${RED}FAILED${NC}"
            cat "/tmp/sri_benchmark_output_$i.txt"
            return 1
        fi
    done

    # Calculate average
    local avg_time=$(echo "scale=4; $total_time / $runs" | bc -l)

    # Store results
    echo "$benchmark_name,$mode,$avg_time,$min_time,$max_time" >> "$OUTPUT_FILE.csv"

    # Display summary
    echo -e "  ${GREEN}Average: ${avg_time}s${NC}"
    echo -e "  ${YELLOW}Min: ${min_time}s, Max: ${max_time}s${NC}"
    echo

    # Clean up temp files
    rm -f /tmp/sri_benchmark_output_*.txt
}

# Function to generate system info
generate_system_info() {
    echo "=== System Information ==="
    echo "Date: $TIMESTAMP"
    echo "Execution Mode: $EXECUTION_MODE"
    echo "OS: $(uname -s) $(uname -r)"
    echo "Architecture: $(uname -m)"
    echo "CPU: $(sysctl -n machdep.cpu.brand_string 2>/dev/null || grep 'model name' /proc/cpuinfo | head -1 | cut -d: -f2 | xargs || echo 'Unknown')"
    echo "Memory: $(sysctl -n hw.memsize 2>/dev/null | awk '{print $1/1024/1024/1024 " GB"}' || grep MemTotal /proc/meminfo | awk '{print $2/1024/1024 " GB"}' || echo 'Unknown')"
    echo "Java Version: $(java -version 2>&1 | head -1)"
    echo "Leiningen Version: $(lein version 2>/dev/null || echo 'Not found')"

    # Check availability of different execution modes
    echo
    echo "=== Execution Mode Availability ==="
    if [[ -f "$NATIVE_BINARY" ]]; then
        echo "Native Binary: Available ($NATIVE_BINARY)"
    else
        echo "Native Binary: Not found ($NATIVE_BINARY)"
    fi

    if [[ -f "$JAR_FILE" ]]; then
        echo "JAR File: Available ($JAR_FILE)"
    else
        echo "JAR File: Not found ($JAR_FILE)"
    fi

    if command -v lein &> /dev/null; then
        echo "Leiningen: Available"
    else
        echo "Leiningen: Not found"
    fi
    echo
}

# Function to generate performance report
generate_report() {
    local csv_file="$OUTPUT_FILE.csv"

    echo "=== Performance Summary ==="
    echo "Benchmark Results (averaged over 3 runs):"
    echo

    printf "%-25s %-10s %-12s %-12s %-12s\n" "Benchmark" "Mode" "Avg Time(s)" "Min Time(s)" "Max Time(s)"
    printf "%-25s %-10s %-12s %-12s %-12s\n" "----------" "----" "-----------" "-----------" "-----------"

    # Read CSV and format output
    while IFS=',' read -r name mode avg min max; do
        printf "%-25s %-10s %-12s %-12s %-12s\n" "$name" "$mode" "$avg" "$min" "$max"
    done < "$csv_file"

    echo

    # Find fastest and slowest
    local fastest=$(sort -t',' -k3 -n "$csv_file" | head -1)
    local slowest=$(sort -t',' -k3 -nr "$csv_file" | head -1)

    echo "=== Analysis ==="
    echo "Fastest: $(echo $fastest | cut -d',' -f1) ($(echo $fastest | cut -d',' -f2) mode) - $(echo $fastest | cut -d',' -f3)s"
    echo "Slowest: $(echo $slowest | cut -d',' -f1) ($(echo $slowest | cut -d',' -f2) mode) - $(echo $slowest | cut -d',' -f3)s"
    echo

    # Calculate total time
    local total_time=$(awk -F',' '{sum += $3} END {print sum}' "$csv_file")
    echo "Total benchmark time: ${total_time}s"
    echo

    echo "=== Optimization Suggestions ==="

    # Performance hints based on results
    while IFS=',' read -r name mode avg min max; do
        case $name in
            *method_call*)
                echo "- Method calls: Consider optimizing function call overhead and recursion"
                ;;
            *arithmetic*)
                echo "- Arithmetic: Focus on numeric operation efficiency and loop optimization"
                ;;
            *string*)
                echo "- Strings: Consider string concatenation optimization and memory allocation"
                ;;
            *array*)
                echo "- Arrays: Focus on array access patterns and bounds checking efficiency"
                ;;
            *class*)
                echo "- Classes: Consider object creation overhead and method dispatch optimization"
                ;;
            *control_flow*)
                echo "- Control flow: Focus on conditional evaluation and loop performance"
                ;;
        esac
    done < "$csv_file"

    # Mode-specific suggestions
    if [[ "$EXECUTION_MODE" == "all" ]]; then
        echo
        echo "=== Mode Comparison Suggestions ==="
        echo "- Compare native vs JVM startup times for optimization priorities"
        echo "- Native binary should show faster startup, JVM may be faster for compute-heavy tasks"
        echo "- Use native for CLI tools, JVM for long-running processes"
    fi
}

# Main execution
main() {
    echo -e "${GREEN}Sri Benchmark Suite${NC}"
    echo "===================="
    echo

    # Check if we're in the right directory
    if [[ ! -f "$PROJECT_ROOT/project.clj" ]]; then
        echo -e "${RED}Error: Must be run from Sri project root or benchmark directory${NC}"
        exit 1
    fi

    # Change to project root
    cd "$PROJECT_ROOT"

    # Initialize output files
    > "$OUTPUT_FILE"
    > "$OUTPUT_FILE.csv"

    # Generate system info
    generate_system_info | tee -a "$OUTPUT_FILE"

    echo "Starting benchmarks..." | tee -a "$OUTPUT_FILE"
    echo | tee -a "$OUTPUT_FILE"

    # Determine which modes to run
    local modes_to_run=()
    case "$EXECUTION_MODE" in
        "all")
            modes_to_run=("lein" "native" "jar")
            ;;
        *)
            modes_to_run=("$EXECUTION_MODE")
            ;;
    esac

    # Run each benchmark in each mode
    local failed_benchmarks=()
    for mode in "${modes_to_run[@]}"; do
        echo -e "${GREEN}=== Running benchmarks in $mode mode ===${NC}" | tee -a "$OUTPUT_FILE"
        echo | tee -a "$OUTPUT_FILE"

        # Check if mode is available
        case "$mode" in
            "native")
                if [[ ! -f "$NATIVE_BINARY" ]]; then
                    echo -e "${YELLOW}Warning: Native binary not found, skipping native mode${NC}" | tee -a "$OUTPUT_FILE"
                    echo "Run './build-native.sh' to build the native binary" | tee -a "$OUTPUT_FILE"
                    echo | tee -a "$OUTPUT_FILE"
                    continue
                fi
                ;;
            "jar")
                if [[ ! -f "$JAR_FILE" ]]; then
                    echo -e "${YELLOW}Warning: JAR file not found, skipping jar mode${NC}" | tee -a "$OUTPUT_FILE"
                    echo "Run 'lein uberjar' to build the JAR file" | tee -a "$OUTPUT_FILE"
                    echo | tee -a "$OUTPUT_FILE"
                    continue
                fi
                ;;
            "lein")
                if ! command -v lein &> /dev/null; then
                    echo -e "${YELLOW}Warning: Leiningen not found, skipping lein mode${NC}" | tee -a "$OUTPUT_FILE"
                    echo | tee -a "$OUTPUT_FILE"
                    continue
                fi
                ;;
        esac

        for benchmark in "${BENCHMARKS[@]}"; do
            if [[ -f "benchmark/$benchmark" ]]; then
                if ! run_benchmark "$benchmark" "$mode" 2>&1 | tee -a "$OUTPUT_FILE"; then
                    failed_benchmarks+=("$benchmark ($mode)")
                fi
            else
                echo -e "${YELLOW}Warning: benchmark/$benchmark not found, skipping...${NC}" | tee -a "$OUTPUT_FILE"
            fi
        done

        echo | tee -a "$OUTPUT_FILE"
    done

    # Generate final report
    generate_report | tee -a "$OUTPUT_FILE"

    # Summary
    echo "=== Summary ===" | tee -a "$OUTPUT_FILE"
    if [[ ${#failed_benchmarks[@]} -eq 0 ]]; then
        echo -e "${GREEN}All benchmarks completed successfully!${NC}" | tee -a "$OUTPUT_FILE"
    else
        echo -e "${RED}Failed benchmarks: ${failed_benchmarks[*]}${NC}" | tee -a "$OUTPUT_FILE"
    fi

    echo | tee -a "$OUTPUT_FILE"
    echo "Full results saved to: $OUTPUT_FILE" | tee -a "$OUTPUT_FILE"
    echo "CSV data saved to: $OUTPUT_FILE.csv" | tee -a "$OUTPUT_FILE"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [mode] [output_file]"
    echo
    echo "Modes:"
    echo "  lein    - Run using 'lein run' (default)"
    echo "  native  - Run using native binary (./target/sri)"
    echo "  jar     - Run using JAR file (java -jar ...)"
    echo "  all     - Run all available modes for comparison"
    echo
    echo "Examples:"
    echo "  $0                           # Run with lein, output to benchmark_results.txt"
    echo "  $0 native                    # Run with native binary"
    echo "  $0 all benchmark_comparison  # Run all modes, output to benchmark_comparison.txt"
    echo
}

# Check dependencies
check_dependencies() {
    if ! command -v bc &> /dev/null; then
        echo -e "${RED}Error: bc calculator not found. Please install bc first.${NC}"
        exit 1
    fi

    # Only check lein if we're using lein mode
    if [[ "$EXECUTION_MODE" == "lein" || "$EXECUTION_MODE" == "all" ]] && ! command -v lein &> /dev/null; then
        echo -e "${YELLOW}Warning: Leiningen not found. Lein mode will be skipped.${NC}"
    fi
}

# Handle help flag
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_usage
    exit 0
fi

check_dependencies
main "$@"
