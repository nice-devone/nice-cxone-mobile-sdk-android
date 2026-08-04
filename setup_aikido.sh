#!/usr/bin/env bash
# Aikido AutoFix init hook.
#
# Appends gradle.properties in user home with a property that settings.gradle.kts detects to relax repository
# resolution from FAIL_ON_PROJECT_REPOS to PREFER_SETTINGS, allowing Aikido
# to inject its dependency mirror at the project level.
set -euo pipefail

GRADLE_PROPS="$HOME/.gradle/gradle.properties"
PROPERTY="aikido.autoFix"

if [ -f "$GRADLE_PROPS" ] && grep -q "^${PROPERTY}=" "$GRADLE_PROPS"; then
    if grep -q "^${PROPERTY}=true$" "$GRADLE_PROPS"; then
        echo "Property '${PROPERTY}=true' already present in ${GRADLE_PROPS} — skipping."
        exit 0
    fi

    echo "Property '${PROPERTY}' is already present in ${GRADLE_PROPS} with a value other than 'true'."
    exit 1  # non-zero signals to Aikido that the environment is in an unexpected state
fi

mkdir -p "$(dirname "$GRADLE_PROPS")"
# If the file exists and is non-empty but lacks a trailing newline, appending would
# merge the new property onto the last existing line. Ensure a clean line boundary.
if [ -s "$GRADLE_PROPS" ] && [ "$(tail -c1 "$GRADLE_PROPS" | wc -l)" -eq 0 ]; then
    printf '\n' >> "$GRADLE_PROPS"
fi
echo "${PROPERTY}=true" >> "$GRADLE_PROPS"
echo "Property '${PROPERTY}=true' appended to ${GRADLE_PROPS}."
