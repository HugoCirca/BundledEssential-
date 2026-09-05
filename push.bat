@echo off
git stash push -m "local bin outputs" -- bin/
git pull --rebase origin main
git stash pop
git push origin main
