#!/usr/bin/env bash
set -euo pipefail
SHA="$1"
BASE=/home/ec2-user/simple-blog
DEST="$BASE/releases/$SHA"

sudo mkdir -p "$DEST"
sudo aws s3 sync "s3://simple-blog-deploy/be/$SHA/" "$DEST/"
sudo chown -R ec2-user:ec2-user "$DEST"
sudo ln -sfn "$DEST" "$BASE/current"
sudo chown ec2-user:ec2-user "$BASE/current"
sudo -u ec2-user $BASE/blog.sh restart

# 최근 5개 릴리스만 유지
sudo ls -1dt "$BASE"/releases/* | tail -n +6 | xargs -r rm -rf
