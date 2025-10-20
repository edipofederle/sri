#!/bin/bash

# Script to run all specs listed in specs-to-run.txt
# Usage: ./run-specs.sh

SPECS_FILE="specs-to-run.txt"
TOTAL_PASSED=0
TOTAL_FAILED=0
FAILED_SPECS=()

# Check if specs file exists
if [ ! -f "$SPECS_FILE" ]; then
    echo "Error: $SPECS_FILE not found!"
    exit 1
fi

echo "=========================================="
echo "Running SRI Ruby Specs"
echo "=========================================="
echo

# Read each spec file from specs-to-run.txt
while IFS= read -r spec_path <&3 || [ -n "$spec_path" ]; do
    # Skip empty lines and comments
    if [[ -z "$spec_path" || "$spec_path" =~ ^[[:space:]]*# ]]; then
        continue
    fi
    
    # Remove any trailing whitespace
    spec_path=$(echo "$spec_path" | sed 's/[[:space:]]*$//')
    
    echo "Running: $spec_path"
    echo "----------------------------------------"
    
    # Run the spec and capture the output
    output=$(lein run -m sri.spec-runner "$spec_path" 2>&1)
    exit_code=$?
    
    echo "$output"
    
    # Extract pass/fail counts from the output
    if echo "$output" | grep -q "Results:"; then
        results_line=$(echo "$output" | grep "Results:" | tail -1)
        passed=$(echo "$results_line" | sed -n 's/.*Results: \([0-9]*\) passed.*/\1/p')
        failed=$(echo "$results_line" | sed -n 's/.*Results: [0-9]* passed, \([0-9]*\) failed.*/\1/p')
        
        # Handle case where there are no failures mentioned
        if [[ -z "$failed" ]]; then
            failed=0
        fi
        
        TOTAL_PASSED=$((TOTAL_PASSED + passed))
        TOTAL_FAILED=$((TOTAL_FAILED + failed))
        
        if [[ $failed -gt 0 ]]; then
            FAILED_SPECS+=("$spec_path")
        fi
    else
        echo "Warning: Could not parse results for $spec_path"
        FAILED_SPECS+=("$spec_path")
    fi
    
    echo
done 3< "$SPECS_FILE"

echo "=========================================="
echo "SUMMARY"
echo "=========================================="
echo "Total tests passed: $TOTAL_PASSED"
echo "Total tests failed: $TOTAL_FAILED"
echo

if [[ ${#FAILED_SPECS[@]} -gt 0 ]]; then
    echo "Specs with failures:"
    for spec in "${FAILED_SPECS[@]}"; do
        echo "  - $spec"
    done
    echo
    exit 1
else
    echo "All specs passed! 🎉"
    exit 0
fi