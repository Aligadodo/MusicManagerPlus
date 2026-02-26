#!/bin/bash

echo "Script path: $0"
echo "Dirname of script: $(dirname "$0")"
echo "Parent dir: $(dirname "$(dirname "$0")")"
echo "Grandparent dir: $(dirname "$(dirname "$(dirname "$0")")")"
