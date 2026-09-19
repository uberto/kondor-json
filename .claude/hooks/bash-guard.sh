#!/usr/bin/env bash
# PreToolUse hook for Bash: blocks push/publish/tag/credential access regardless
# of how the command is spelled (bash -c, env, full paths, per-module tasks...).
# Receives the tool-call JSON on stdin; exit code 2 blocks the call.
# Defense-in-depth alongside the permission rules in .claude/settings.json.

input=$(cat)

block() {
    echo "bash-guard: blocked — $1" >&2
    exit 2
}

# git subcommands that publish state or create versions (options allowed between git and subcommand)
GIT_SUB='git([[:space:]]+-[^[:space:]]+([[:space:]]+[^-[:space:]][^[:space:]]*)?)*[[:space:]]+'
echo "$input" | grep -qE "${GIT_SUB}push([^a-zA-Z_-]|$)"  && block "git push is never allowed"
echo "$input" | grep -qE "${GIT_SUB}tag([^a-zA-Z_-]|$)"   && block "git tag is never allowed (versions are created manually by the user)"
echo "$input" | grep -qE "${GIT_SUB}update-ref[^,]*refs/tags" && block "creating tag refs is never allowed"
echo "$input" | grep -qE "${GIT_SUB}config[^,]*alias"     && block "defining git aliases could bypass command rules"

# gradle publishing (any module, any spelling); publishToMavenLocal alone is fine
if echo "$input" | grep -qE 'gradlew?[^,]*publish'; then
    echo "$input" | sed 's/publishToMavenLocal//g' | grep -qE 'gradlew?[^,]*publish' \
        && block "gradle publish tasks are never allowed (publishToMavenLocal is permitted)"
fi

# GitHub CLI escalation paths
echo "$input" | grep -qE 'gh[[:space:]]+(release|api|pr[[:space:]]+merge)' && block "gh release/api/pr-merge are never allowed"

# credential files
echo "$input" | grep -qE '\.gradle/gradle\.properties|\.ssh/|\.gnupg' && block "credential files are off-limits"

# tampering with the policy files via shell (git add/diff of them is fine)
echo "$input" | grep -qE '(>>?[[:space:]]*[^[:space:]]*|rm[^,]*|sed[^,]*-i[^,]*|tee[[:space:]][^,]*|mv[^,]*)\.claude/' \
    && block "modifying .claude/ from the shell requires explicit user permission"

exit 0
