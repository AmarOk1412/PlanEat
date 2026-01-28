#!/bin/bash

# PO Workflow Trigger Script
# Usage: ./po-workflow.sh <action> [options]
# Requires: GITHUB_TOKEN environment variable set

set -euo pipefail

# Configuration
REPO_OWNER="AmarOk1412"
REPO_NAME="PlanEat"
WORKFLOW_FILE="po.yaml"
GITHUB_API="https://api.github.com"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_error() {
    echo -e "${RED}❌ Error: $1${NC}" >&2
    exit 1
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

validate_token() {
    if [[ -z "${GITHUB_TOKEN:-}" ]]; then
        log_error "GITHUB_TOKEN environment variable is not set"
    fi
}

show_usage() {
    cat << EOF
Usage: $0 <action> [options]

Actions:
  add-label       Add a label to an issue
  create-issue    Create a new issue from template
  close-issue     Close an issue as Done

Examples:
  # Add label to issue #42
  $0 add-label --issue 42 --label bug

  # Create a new bug report
  $0 create-issue --title "Bug description" --type bug --priority high --description "Full description"

  # Close issue #42 as done
  $0 close-issue --issue 42

Options:
  --issue <number>       Issue number (required for add-label, close-issue)
  --label <name>         Label to add (required for add-label)
  --title <string>       Issue title (required for create-issue)
  --type <type>          Issue type: bug|feature|task (required for create-issue)
  --priority <level>     Priority: critical|high|medium|low (required for create-issue)
  --description <text>   Issue description (required for create-issue)
  --help                 Show this help message

EOF
}

# Parse arguments
if [[ $# -lt 1 ]]; then
    show_usage
    exit 1
fi

ACTION="$1"
shift

# Initialize variables
ISSUE_NUMBER=""
LABEL=""
ISSUE_TITLE=""
ISSUE_TYPE=""
PRIORITY=""
DESCRIPTION=""

# Parse optional arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --issue)
            ISSUE_NUMBER="$2"
            shift 2
            ;;
        --label)
            LABEL="$2"
            shift 2
            ;;
        --title)
            ISSUE_TITLE="$2"
            shift 2
            ;;
        --type)
            ISSUE_TYPE="$2"
            shift 2
            ;;
        --priority)
            PRIORITY="$2"
            shift 2
            ;;
        --description)
            DESCRIPTION="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            ;;
    esac
done

# Validate token
validate_token

# Build workflow inputs JSON using jq for proper escaping
build_inputs_json() {
    local action=$1

    case $action in
        add-label)
            [[ -z "$ISSUE_NUMBER" ]] && log_error "add-label requires --issue"
            [[ -z "$LABEL" ]] && log_error "add-label requires --label"
            jq -n \
                --arg action "add-label" \
                --arg issue_number "$ISSUE_NUMBER" \
                --arg label "$LABEL" \
                '{action: $action, issue_number: $issue_number, label: $label}'
            ;;
        create-issue)
            [[ -z "$ISSUE_TITLE" ]] && log_error "create-issue requires --title"
            [[ -z "$ISSUE_TYPE" ]] && log_error "create-issue requires --type"
            [[ -z "$PRIORITY" ]] && log_error "create-issue requires --priority"
            [[ -z "$DESCRIPTION" ]] && log_error "create-issue requires --description"
            jq -n \
                --arg action "create-issue" \
                --arg issue_title "$ISSUE_TITLE" \
                --arg issue_type "$ISSUE_TYPE" \
                --arg issue_priority "$PRIORITY" \
                --arg issue_description "$DESCRIPTION" \
                '{action: $action, issue_title: $issue_title, issue_type: $issue_type, issue_priority: $issue_priority, issue_description: $issue_description}'
            ;;
        close-issue)
            [[ -z "$ISSUE_NUMBER" ]] && log_error "close-issue requires --issue"
            jq -n \
                --arg action "close-issue" \
                --arg issue_number "$ISSUE_NUMBER" \
                '{action: $action, issue_number: $issue_number}'
            ;;
        *)
            log_error "Unknown action: $action"
            ;;
    esac
}

# Trigger workflow
trigger_workflow() {
    local action=$1
    local inputs=$(build_inputs_json "$action")

    log_info "Triggering workflow: $action"
    log_info "Repository: $REPO_OWNER/$REPO_NAME"

    local response=$(curl -s -X POST \
        -H "Authorization: token $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        -d "{\"ref\": \"main\", \"inputs\": $inputs}" \
        "$GITHUB_API/repos/$REPO_OWNER/$REPO_NAME/actions/workflows/$WORKFLOW_FILE/dispatches")

    # Check for errors
    if echo "$response" | grep -q "message"; then
        log_error "GitHub API error: $response"
    fi

    log_success "Workflow triggered successfully!"
    log_info "Check GitHub Actions tab for progress: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
}

# Main execution
case $ACTION in
    add-label|create-issue|close-issue)
        trigger_workflow "$ACTION"
        ;;
    *)
        log_error "Unknown action: $ACTION"
        ;;
esac