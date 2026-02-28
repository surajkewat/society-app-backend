#!/bin/bash
# Run this from the project root to stage and commit the rename to society-app-backend

set -e
cd "$(dirname "$0")"

echo "=== Current git status (before add) ==="
git status

echo ""
echo "=== Staging all changes (modified, deleted, new files) ==="
git add -A

echo ""
echo "=== Status after git add -A ==="
git status

echo ""
echo "=== Creating commit ==="
git commit -m "Rename to society-app-backend: new package com.scube.society_app_backend, updated pom and config"

echo ""
echo "=== Done. Latest commit ==="
git log -1 --oneline

echo ""
echo "Push with: git push -u origin main"
