#!/usr/bin/env bash

set -eu

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

tmp_file="$(mktemp)"
trap 'rm -f "$tmp_file"' EXIT

git ls-files \
  ':!**/.env.example' \
  ':!**/.env.*.example' \
  ':!**/docs/**' \
  ':!**/seed/**' \
  ':!**/*.md' \
  ':!**/*.json' \
  ':!**/*.svg' \
  ':!**/*.png' \
  ':!**/*.jpg' \
  ':!**/*.jpeg' \
  ':!**/*.gif' \
  ':!**/*.lock' \
  > "$tmp_file"

if [ ! -s "$tmp_file" ]; then
  echo "No tracked files to scan."
  exit 0
fi

patterns="(AKIA[0-9A-Z]{16}|-----BEGIN (RSA|DSA|EC|OPENSSH|PRIVATE) KEY-----|AIza[0-9A-Za-z_-]{35}|sk_(live|test)_[0-9A-Za-z]{16,}|ghp_[0-9A-Za-z]{36}|github_pat_[0-9A-Za-z_]{20,}|xox[baprs]-[0-9A-Za-z-]{10,}|(?i)(password|passwd|pwd|secret|token|api[_-]?key|access[_-]?key|private[_-]?key)\\s*[:=]\\s*[\"'][^\"']{8,}[\"'])"

found=0
while IFS= read -r path; do
  if rg -n --pcre2 "$patterns" "$path"; then
    found=1
  fi
done < "$tmp_file"

if [ "$found" -eq 1 ]; then
  echo
  echo "Potential secrets detected. Review the findings above."
  exit 1
fi

echo "No secret patterns detected."
