#!/bin/bash
# Performance Test Runner for Discipolat/Church OS
# Usage: ./run-performance-tests.sh [k6|jmeter|both] [environment]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

MODE=${1:-k6}
ENV=${2:-local}

BASE_URL=${BASE_URL:-"http://localhost:8080"}
TENANT_ID=${TENANT_ID:-"00000000-0000-0000-0000-000000000001"}
JWT_TOKEN=${JWT_TOKEN:-""}

echo "========================================="
echo "Discipolat Performance Test Runner"
echo "========================================="
echo "Mode: $MODE"
echo "Environment: $ENV"
echo "Base URL: $BASE_URL"
echo "Tenant ID: $TENANT_ID"
echo ""

# Check if k6 is installed
check_k6() {
    if ! command -v k6 &> /dev/null; then
        echo "k6 not found. Installing..."
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            sudo apt-get update && sudo apt-get install -y gpg
            gpg -k
            gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
            echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
            sudo apt-get update
            sudo apt-get install -y k6
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            brew install k6
        else
            echo "Please install k6 manually: https://k6.io/docs/getting-started/installation/"
            exit 1
        fi
    fi
}

# Check if JMeter is installed
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        echo "JMeter not found. Please install JMeter 5.6+"
        echo "Download from: https://jmeter.apache.org/download_jmeter.cgi"
        exit 1
    fi
}

# Generate test data
generate_data() {
    echo "Generating test data..."
    cd "$SCRIPT_DIR"
    python3 generate_data.py --tenants 10 --persons 1000 --spaces 20 --events 50 --assets 20 --output performance-data
}

# Run k6 tests
run_k6() {
    check_k6
    echo "Running k6 load tests..."
    
    cd "$SCRIPT_DIR"
    
    # Export environment variables for k6
    export BASE_URL="$BASE_URL"
    export TENANT_ID="$TENANT_ID"
    export JWT_TOKEN="$JWT_TOKEN"
    
    # Run with different scenarios
    echo "Running smoke test..."
    k6 run --vus 1 --duration 30s k6-load-test.js
    
    echo "Running load test..."
    k6 run k6-load-test.js --out json=results/k6-results.json --summary-export=results/k6-summary.json
    
    echo "Running spike test..."
    k6 run --vus 200 --duration 2m k6-load-test.js --out json=results/k6-spike-results.json
    
    echo "Running stress test..."
    k6 run --vus 500 --duration 5m k6-load-test.js --out json=results/k6-stress-results.json
}

# Run JMeter tests
run_jmeter() {
    check_jmeter
    echo "Running JMeter load tests..."
    
    cd "$SCRIPT_DIR"
    
    mkdir -p results
    
    jmeter -n -t discipolat-load-test.jmx \
        -J BASE_URL="$BASE_URL" \
        -J TENANT_ID="$TENANT_ID" \
        -J JWT_TOKEN="$JWT_TOKEN" \
        -l results/jmeter-results.jtl \
        -e -o results/jmeter-report \
        -j results/jmeter.log
}

# Generate performance report
generate_report() {
    echo "Generating performance report..."
    
    cd "$SCRIPT_DIR"
    cat > results/PERFORMANCE_REPORT.md << EOF
# Discipolat/Church OS Performance Test Report

**Test Date:** $(date)
**Environment:** $ENV
**Base URL:** $BASE_URL
**Tenant ID:** $TENANT_ID

## Test Configuration

| Parameter | Value |
|-----------|-------|
| Test Mode | $MODE |
| Concurrent Users | 100 (ramp to 200) |
| Test Duration | ~10 minutes |
| Target Latency (p95) | < 500ms |
| Target Error Rate | < 1% |

## Results Summary

EOF

    if [[ "$MODE" == "k6" || "$MODE" == "both" ]]; then
        if [[ -f results/k6-summary.json ]]; then
            echo "### k6 Load Test Results" >> results/PERFORMANCE_REPORT.md
            echo "" >> results/PERFORMANCE_REPORT.md
            cat results/k6-summary.json | jq -r '
                "#### Metrics\n",
                "| Metric | Value |",
                "|--------|-------|",
                "| Total Requests | \(.metrics.http_reqs.values.count) |",
                "| Failed Requests | \(.metrics.http_req_failed.values.passes) |",
                "| Error Rate | \(.metrics.http_req_failed.values.rate * 100)% |",
                "| Avg Latency | \(.metrics.http_req_duration.values.avg | round)ms |",
                "| p50 Latency | \(.metrics.http_req_duration.values["p(50)"] | round)ms |",
                "| p95 Latency | \(.metrics.http_req_duration.values["p(95)"] | round)ms |",
                "| p99 Latency | \(.metrics.http_req_duration.values["p(99)"] | round)ms |",
                "| Max Latency | \(.metrics.http_req_duration.values.max | round)ms |",
                "| Throughput | \(.metrics.http_reqs.values.rate | round) req/s |"
            ' >> results/PERFORMANCE_REPORT.md
            echo "" >> results/PERFORMANCE_REPORT.md
        fi
    fi
    
    if [[ "$MODE" == "jmeter" || "$MODE" == "both" ]]; then
        echo "### JMeter Load Test Results" >> results/PERFORMANCE_REPORT.md
        echo "" >> results/PERFORMANCE_REPORT.md
        echo "See [JMeter HTML Report](jmeter-report/index.html) for detailed results." >> results/PERFORMANCE_REPORT.md
        echo "" >> results/PERFORMANCE_REPORT.md
    fi
    
    # Budget compliance
    cat >> results/PERFORMANCE_REPORT.md << EOF

## Budget Compliance

| Budget | Target | Status |
|--------|--------|--------|
| Dashboard p95 latency | < 500ms | ⏳ |
| Space Bootstrap p95 latency | < 1000ms | ⏳ |
| Global Search p95 latency | < 300ms | ⏳ |
| People Directory p95 latency | < 300ms | ⏳ |
| Calendar p95 latency | < 500ms | ⏳ |
| Mobile Sync p95 latency | < 2000ms | ⏳ |
| Overall Error Rate | < 1% | ⏳ |

## Recommendations

1. **Database Indexes**: Review slow query logs and add missing indexes
2. **N+1 Queries**: Check for N+1 patterns in Space bootstrap and People directory
3. **Caching**: Implement Redis caching for dashboard KPIs and space configurations
4. **Pagination**: Use cursor-based pagination for large datasets
5. **Connection Pooling**: Tune HikariCP pool size for concurrent load
6. **Virtual Scrolling**: Ensure frontend uses virtualization for lists > 100 items

## Next Steps

1. Fix any budget violations
2. Re-run tests after optimizations
3. Document findings in architecture decision records
4. Set up continuous performance testing in CI/CD

---

*Report generated automatically by Discipolat Performance Test Suite*
EOF

    echo "Performance report generated: results/PERFORMANCE_REPORT.md"
}

# Main execution
main() {
    mkdir -p "$SCRIPT_DIR/results"
    
    case $MODE in
        k6)
            generate_data
            run_k6
            generate_report
            ;;
        jmeter)
            generate_data
            run_jmeter
            generate_report
            ;;
        both)
            generate_data
            run_k6
            run_jmeter
            generate_report
            ;;
        *)
            echo "Usage: $0 [k6|jmeter|both] [environment]"
            exit 1
            ;;
    esac
    
    echo ""
    echo "========================================="
    echo "Performance tests completed!"
    echo "Results in: $SCRIPT_DIR/results/"
    echo "========================================="
}

main "$@"