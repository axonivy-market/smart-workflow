#!/usr/bin/env bash
set -euo pipefail

TARGET_FILE="smart-workflow-test/config/variables.yaml"
TEMPLATE_FILE=".devcontainer/templates/smart-workflow-test.variables.yaml"

if [[ ! -f "$TARGET_FILE" ]]; then
  mkdir -p "$(dirname "$TARGET_FILE")"
  cp "$TEMPLATE_FILE" "$TARGET_FILE"
fi

mvn clean package --batch-mode \
  -Dmaven.test.skip=true \
  -Divy.script.validation.skip=true \
  -Divy.engine.directory=/usr/lib/axonivy-engine
