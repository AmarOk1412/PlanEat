#!/usr/bin/env bash
set -euo pipefail

# senior-developer-fetch.sh
# Fetch the latest issue labeled "bug" or "feature request" and print title + description.
# Requirements: `gh` (GitHub CLI) and `jq` installed.

usage() {
  cat <<EOF
Usage: $0 [owner/repo] [--include-closed]

If owner/repo is omitted the script will try to infer it from GITHUB_REPOSITORY
or the local git remote named 'origin'.
EOF
  exit 2
}

INCLUDE_CLOSED=false
REPO_ARG=""
for arg in "$@"; do
  case "$arg" in
    --include-closed) INCLUDE_CLOSED=true ;;
    -h|--help) usage ;;
    *) REPO_ARG="$arg" ;;
  esac
done

if [ -n "$REPO_ARG" ]; then
  REPO="$REPO_ARG"
elif [ -n "${GITHUB_REPOSITORY-}" ]; then
  REPO="$GITHUB_REPOSITORY"
else
  origin_url=$(git remote get-url origin 2>/dev/null || true)
  if [ -z "$origin_url" ]; then
    echo "Error: could not determine repository. Provide owner/repo or set GITHUB_REPOSITORY." >&2
    usage
  fi
  # Convert various git remote url formats to owner/repo
  if [[ "$origin_url" =~ github.com[:/]+([^/]+/[^/.]+) ]]; then
    REPO="${BASH_REMATCH[1]}"
  else
    echo "Error: unsupported remote URL: $origin_url" >&2
    exit 3
  fi
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "Error: gh (GitHub CLI) is required. Install from https://cli.github.com/" >&2
  exit 4
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "Error: jq is required for JSON parsing. Install via your package manager." >&2
  exit 5
fi

state="open"
if [ "$INCLUDE_CLOSED" = true ]; then
  state="all"
fi

query="repo:${REPO} is:issue state:${state} (label:bug OR label:\"feature request\")"
echo "Searching: $query" >&2

resp=$(gh api -X GET search/issues -F q="$query" -f sort=created -f order=desc -f per_page=1)

total=$(echo "$resp" | jq -r '.total_count // 0')
if [ "$total" -eq 0 ]; then
  echo "Found: false"
  exit 0
fi

title=$(echo "$resp" | jq -r '.items[0].title // ""')
body=$(echo "$resp" | jq -r '.items[0].body // ""')
url=$(echo "$resp" | jq -r '.items[0].html_url // ""')

echo "Found: true"
echo "Title: $title"
echo "URL: $url"
echo "Body:"
echo "$body"
