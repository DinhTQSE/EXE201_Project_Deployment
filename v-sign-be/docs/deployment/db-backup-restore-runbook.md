# Database Backup And Restore Runbook

Use this runbook before production deploys, migrations, and rollback drills.

## Required Variables

Set these in the shell or deployment secret manager:

```powershell
$env:DB_URL="postgresql://host:5432/database"
$env:DB_USER="vsign"
$env:DB_PASSWORD="change-me"
$env:DB_SCHEMA="public"
$env:BACKUP_DIR="D:\vsign-backups"
```

## Backup

Create a timestamped PostgreSQL custom-format backup:

```powershell
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backup = Join-Path $env:BACKUP_DIR "vsign-$timestamp.dump"
pg_dump --format=custom --verbose --no-owner --no-acl --schema=$env:DB_SCHEMA --file=$backup $env:DB_URL
```

Record the app image tag, git commit, Flyway version, and backup file path in the deployment notes.

## Verify Backup

List the archive contents:

```powershell
pg_restore --list $backup
```

The output must include the expected schema, tables, and Flyway history table.

## Restore To A Staging Database

Never restore directly to production unless rollback has been approved.

```powershell
$env:RESTORE_URL="postgresql://host:5432/vsign_restore"
pg_restore --clean --if-exists --no-owner --no-acl --dbname=$env:RESTORE_URL $backup
```

After restore, start the backend against the restored database and check:

```powershell
curl http://localhost:8080/V-sign/api/v1/health
curl http://localhost:8080/V-sign/api/v1/version
```

## Production Rollback

1. Stop write traffic at the reverse proxy.
2. Stop backend and AI containers.
3. Restore the last verified backup to the production database.
4. Deploy the previous known-good backend, frontend, and AI images.
5. Run the deployment smoke checklist.
6. Re-enable write traffic only after health checks and login smoke tests pass.

## Retention

Keep at least:

- 7 daily backups.
- 4 weekly backups.
- 3 monthly backups.

Store production backups outside the application host.
