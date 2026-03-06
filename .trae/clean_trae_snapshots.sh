#!/bin/bash
SNAPSHOT_DIR="$HOME/Library/Application Support/Trae CN/ModularData/ai-agent/snapshot"

if [ -d "$SNAPSHOT_DIR" ]; then
    echo "清理 Trae 快照文件..."
    rm -rf "$SNAPSHOT_DIR"/*
    echo "清理完成"
else
    echo "快照目录不存在"
fi