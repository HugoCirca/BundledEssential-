# Updater

Checks GitHub Releases on startup. If new: download to `plugins/update/` → next restart replaces jar. Checksum-verified, backup in `plugins/BundledEssential-backups/` (last 3), auto-restore on fail. `/bundledupdate` shows pending. Permission `bundleessential.update`.
