#!/usr/bin/env bash
set -euo pipefail
SHA="$1"
BASE=/var/www/chidoo
DEST="$BASE/releases/$SHA"

sudo mkdir -p "$DEST"
sudo aws s3 sync "s3://simple-blog-deploy/fe/$SHA/" "$DEST/"
sudo chown -R ec2-user:nginx "$DEST"
sudo ln -sfn "$DEST" "$BASE/current"

# 최근 5개 릴리스만 유지
sudo ls -1dt "$BASE"/releases/* | tail -n +6 | xargs -r rm -rf
