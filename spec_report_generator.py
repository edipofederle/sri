#!/usr/bin/env python3

import os
import subprocess
import datetime
from pathlib import Path


def count_all_spec_files():
    """Count all *_spec.rb files in spec/ directory"""
    spec_dir = Path("spec")
    if not spec_dir.exists():
        return 0

    spec_files = list(spec_dir.rglob("*_spec.rb"))
    return len(spec_files)


def count_all_individual_tests():
    """Count all individual 'it' tests in all spec files"""
    spec_dir = Path("spec")
    if not spec_dir.exists():
        return 0

    total_tests = 0
    spec_files = list(spec_dir.rglob("*_spec.rb"))

    for spec_file in spec_files:
        try:
            with open(spec_file, "r", encoding="utf-8") as f:
                content = f.read()
                # Count all occurrences of 'it "' which indicates individual tests
                test_count = content.count('it "')
                total_tests += test_count
        except Exception as e:
            print(f"Error reading {spec_file}: {e}")

    return total_tests


def get_configured_specs():
    """Get list of configured specs from ruby-specs-to-run.edn"""
    try:
        with open("ruby-specs-to-run.edn", "r") as f:
            content = f.read()

        # Simple parsing of EDN format
        specs = []
        for line in content.split("\n"):
            line = line.strip()
            if line.startswith('{:path "'):
                path = line[8:-2]  # Extract path from {:path "..."}
                specs.append(path)
        return specs
    except FileNotFoundError:
        return []


def parse_spec_output(spec_path):
    """Parse the output of running a single spec and return results"""
    try:
        cmd = f'lein run test_rspec_capabilities.rb "{spec_path}"'
        result = subprocess.run(
            cmd, shell=True, capture_output=True, text=True, timeout=300
        )
        output = result.stdout

        lines = output.split("\n")
        pass_count = sum(1 for line in lines if "(pass)" in line)
        fail_count = sum(1 for line in lines if "(fail)" in line)
        error_count = sum(1 for line in lines if "(error)" in line)

        return {
            "path": spec_path,
            "pass": pass_count,
            "fail": fail_count,
            "error": error_count,
            "raw_output": output,
        }
    except subprocess.TimeoutExpired:
        return {
            "path": spec_path,
            "pass": 0,
            "fail": 0,
            "error": 1,
            "raw_output": "TIMEOUT",
        }
    except Exception as e:
        return {
            "path": spec_path,
            "pass": 0,
            "fail": 0,
            "error": 1,
            "raw_output": f"ERROR: {str(e)}",
        }


def get_all_spec_files():
    """Get list of all spec files"""
    spec_dir = Path("spec")
    return list(spec_dir.rglob("*_spec.rb"))


def get_individual_tests_from_file(spec_path):
    """Extract individual test descriptions from a spec file"""
    try:
        with open(spec_path, "r", encoding="utf-8") as f:
            content = f.read()

        tests = []
        lines = content.split("\n")
        for line in lines:
            line = line.strip()
            # Match lines like: it "description" do
            if line.startswith('it "') and '" do' in line:
                test_desc = line[4 : line.find('" do')]
                tests.append(test_desc)

        return tests
    except Exception as e:
        return [f"Error reading file: {str(e)}"]


def run_all_specs():
    """Run all configured specs and collect results"""
    specs = get_configured_specs()
    results = []

    for spec_path in specs:
        print(f"Running: {spec_path}")
        result = parse_spec_output(spec_path)
        results.append(result)
        print(
            f"  Pass: {result['pass']}, Fail: {result['fail']}, Error: {result['error']}"
        )

    return results


def generate_report():
    """Generate HTML report of spec progress"""
    print("Generating spec report...")

    all_specs_count = count_all_spec_files()
    all_individual_tests = count_all_individual_tests()
    configured_specs = get_configured_specs()
    configured_count = len(configured_specs)
    all_spec_files = get_all_spec_files()

    # Only run configured specs (others would take too long)
    results = run_all_specs()

    total_pass = sum(r["pass"] for r in results)
    total_fail = sum(r["fail"] for r in results)
    total_error = sum(r["error"] for r in results)

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # Generate spec rows for ALL spec files
    spec_rows = []

    for spec_file in all_spec_files:
        spec_path = str(spec_file)
        individual_tests = get_individual_tests_from_file(spec_path)
        test_count = len(individual_tests)

        # Check if this spec is configured (has results)
        result = next((r for r in results if r["path"] == spec_path), None)

        if result:
            pass_count = result["pass"]
            fail_count = result["fail"]
            error_count = result["error"]
            status_class = (
                "configured"
                if pass_count + fail_count + error_count > 0
                else "not-configured"
            )
        else:
            pass_count = fail_count = error_count = 0
            status_class = "not-configured"

        total = pass_count + fail_count + error_count

        # Generate individual test details HTML
        test_details = ""
        if individual_tests:
            test_details = "<div class='test-details' style='display:none;'>"
            for i, test_desc in enumerate(
                individual_tests[:50]
            ):  # Limit to first 50 tests for performance
                status = "unknown"
                if result and i < pass_count:
                    status = "pass"
                elif result and i < pass_count + fail_count:
                    status = "fail"
                elif result and i < pass_count + fail_count + error_count:
                    status = "error"

                test_details += (
                    f"<div class='test-item {status}'>it \"{test_desc}\"</div>"
                )

            if len(individual_tests) > 50:
                test_details += f"<div class='test-more'>... and {len(individual_tests) - 50} more tests</div>"
            test_details += "</div>"

        # Check if configured
        configured_status = "✓" if result else "✗"
        row = f"""      <tr class="spec-row" onclick="toggleTests(this)">
        <td class="spec-path">
          <span class="toggle-icon">▶</span>
          <span class="configured-status">{configured_status}</span>
          {spec_path.replace("spec/", "")}
          {test_details}
        </td>
        <td class="pass">{pass_count}</td>
        <td class="fail">{fail_count}</td>
        <td class="error">{error_count}</td>
        <td>{total}</td>
        <td>{test_count}</td>
        <td class="status {status_class}">{"Configured" if result else "Not Configured"}</td>
      </tr>"""
        spec_rows.append(row)

    html_content = f"""<!DOCTYPE html>
<html>
<head>
  <title>Sri Ruby Spec Progress Report</title>
  <style>
    body {{ font-family: Arial, sans-serif; margin: 20px; }}
    .header {{ background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px; }}
    .summary {{ display: flex; gap: 20px; margin-bottom: 20px; }}
    .metric {{ background: white; border: 1px solid #ddd; padding: 15px; border-radius: 5px; text-align: center; }}
    .pass {{ color: green; }}
    .fail {{ color: red; }}
    .error {{ color: orange; }}
    .configured {{ color: blue; }}
    .not-configured {{ color: #999; }}
    table {{ width: 100%; border-collapse: collapse; }}
    th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
    th {{ background: #f2f2f2; }}
    .spec-row {{ cursor: pointer; }}
    .spec-row:hover {{ background: #f9f9f9; }}
    .spec-path {{ font-family: monospace; font-size: 12px; position: relative; }}
    .toggle-icon {{ margin-right: 8px; display: inline-block; width: 12px; }}
    .configured-status {{ margin-right: 8px; font-weight: bold; }}
    .test-details {{ margin-left: 24px; margin-top: 8px; padding: 8px; background: #f8f8f8; border-radius: 3px; font-size: 11px; }}
    .test-item {{ margin: 2px 0; padding: 2px 4px; border-radius: 2px; }}
    .test-item.pass {{ background: #d4edda; color: #155724; }}
    .test-item.fail {{ background: #f8d7da; color: #721c24; }}
    .test-item.error {{ background: #ffeaa7; color: #856404; }}
    .test-item.unknown {{ background: #e2e3e5; color: #383d41; }}
    .test-more {{ font-style: italic; color: #666; margin-top: 4px; }}
  </style>
  <script>
    function toggleTests(row) {{
      var details = row.querySelector('.test-details');
      var icon = row.querySelector('.toggle-icon');
      if (details.style.display === 'none') {{
        details.style.display = 'block';
        icon.textContent = '▼';
      }} else {{
        details.style.display = 'none';
        icon.textContent = '▶';
      }}
    }}
  </script>
</head>
<body>
  <h1>Sri Ruby Spec Progress Report</h1>
  <div class="header">
    <p><strong>Generated:</strong> {timestamp}</p>
    <p><strong>Total Spec Files:</strong> {all_specs_count}</p>
    <p><strong>Total Individual Tests:</strong> {all_individual_tests}</p>
    <p><strong>Configured Specs:</strong> {configured_count}</p>
  </div>
  <div class="summary">
    <div class="metric">
      <h3 class="pass">{total_pass}</h3>
      <p>Pass</p>
    </div>
    <div class="metric">
      <h3 class="fail">{total_fail}</h3>
      <p>Fail</p>
    </div>
    <div class="metric">
      <h3 class="error">{total_error}</h3>
      <p>Error</p>
    </div>
  </div>
  <h2>All Spec Files ({len(all_spec_files)} total)</h2>
  <p>Click on any row to expand and see individual tests</p>
  <table>
    <tr>
      <th>Spec File</th>
      <th>Pass</th>
      <th>Fail</th>
      <th>Error</th>
      <th>Ran</th>
      <th>Total Tests</th>
      <th>Status</th>
    </tr>
{chr(10).join(spec_rows)}
  </table>
</body>
</html>"""

    with open("spec-report.html", "w") as f:
        f.write(html_content)

    print(f"Report generated: spec-report.html")
    print(f"Summary: {total_pass} pass, {total_fail} fail, {total_error} error")

    return {
        "total_specs": all_specs_count,
        "total_individual_tests": all_individual_tests,
        "configured_specs": configured_count,
        "pass": total_pass,
        "fail": total_fail,
        "error": total_error,
        "results": results,
    }


if __name__ == "__main__":
    generate_report()
