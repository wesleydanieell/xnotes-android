#!/usr/bin/env python3
"""Verify the pinned review branches against their declared bases without checking them out."""
import json
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'docs/pr-map/validation.json').read_text())

def git(*args):
    return subprocess.run(['git', *args], cwd=root, text=True, capture_output=True)

failures = []
if git('rev-parse', 'upstream/master').stdout.strip() != manifest['upstream_sha']:
    failures.append('upstream/master changed: refresh the audit before claiming current compatibility')
for entry in manifest['branches']:
    branch, base = entry['branch'], entry['base']
    if git('rev-parse', branch).stdout.strip() != entry['sha']:
        failures.append(f'{branch}: moved since the audit')
        continue
    merge = git('merge-tree', '--write-tree', base, branch)
    ancestry = git('merge-base', '--is-ancestor', manifest['upstream_sha'], branch)
    if merge.returncode or ancestry.returncode:
        failures.append(f'{branch}: conflicts with {base} or missing audited upstream')
    else:
        print(f'OK {branch} -> {base}')
if failures:
    print('\n'.join(failures), file=sys.stderr)
    sys.exit(1)
print('All maintained branches contain their audited upstream and merge with their declared bases.')
