#!/usr/bin/env bash
# ==============================================================================
# Smart Otter - Phase 1 Read-Only VPS Pre-Flight Audit Script
# Safe to run on Ubuntu VPS hosting skillswap-*: makes ZERO modifications.
# ==============================================================================
set -u

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

pass() { printf "[PASS] %s\n" "$1"; PASS_COUNT=$((PASS_COUNT + 1)); }
warn() { printf "[WARN] %s\n" "$1"; WARN_COUNT=$((WARN_COUNT + 1)); }
fail() { printf "[FAIL] %s\n" "$1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }
info() { printf "[INFO] %s\n" "$1"; }

printf "==========================================================\n"
printf " Smart Otter VPS Pre-Flight Check (Read-Only)\n"
printf "==========================================================\n\n"

# 1. Check Docker & Docker Compose v2
if command -v docker >/dev/null 2>&1; then
    if docker info >/dev/null 2>&1; then
        pass "Docker daemon is running ($(docker --version))"
    else
        fail "Docker is installed but current user cannot access Docker daemon (try sudo or docker group)"
    fi
else
    fail "Docker is not installed"
fi

if docker compose version >/dev/null 2>&1; then
    pass "Docker Compose v2 available ($(docker compose version --short 2>/dev/null || echo 'v2'))"
else
    fail "Docker Compose v2 plugin missing (install docker-compose-plugin)"
fi

# 2. Check Memory & Swap & Disk
if command -v free >/dev/null 2>&1; then
    AVAIL_MB=$(awk '/^MemAvailable:/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 0)
    SWAP_MB=$(awk '/^SwapTotal:/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 0)
    if [ "${AVAIL_MB}" -ge 4096 ]; then
        pass "Available RAM: ${AVAIL_MB} MiB (>= 4096 MiB required for 3.75 GiB limits)"
    elif [ "${AVAIL_MB}" -ge 3072 ]; then
        warn "Available RAM: ${AVAIL_MB} MiB (tight against 3.75 GiB max limits)"
    else
        fail "Available RAM too low: ${AVAIL_MB} MiB (< 3072 MiB)"
    fi
    if [ "${SWAP_MB}" -eq 0 ]; then
        warn "Swap is 0 MiB: keep container JVM limits (-XX:MaxRAMPercentage=70.0) intact to prevent OOM"
    else
        pass "Swap available: ${SWAP_MB} MiB"
    fi
fi

AVAIL_DISK_GB=$(df -BG / 2>/dev/null | awk 'NR==2 {gsub("G","",$4); print $4}' || echo 0)
if [ "${AVAIL_DISK_GB}" -ge 10 ] 2>/dev/null; then
    pass "Free root disk space: ${AVAIL_DISK_GB} GiB (>= 10 GiB)"
else
    warn "Check root disk space: ${AVAIL_DISK_GB} GiB free"
fi

# 3. Check Port Isolation
FRONTEND_PORT="${FRONTEND_HOST_PORT:-3100}"
if [ -f .env ]; then
    ENV_PORT=$(grep -E '^FRONTEND_HOST_PORT=' .env | tail -n1 | cut -d'=' -f2- | tr -d '\r"' | xargs 2>/dev/null || true)
    if [ -n "${ENV_PORT}" ]; then
        FRONTEND_PORT="${ENV_PORT}"
    fi
fi

if command -v ss >/dev/null 2>&1; then
    LISTENERS=$(ss -tulpn 2>/dev/null || true)
    if printf "%s\n" "${LISTENERS}" | grep -qE ":${FRONTEND_PORT}[[:space:]]"; then
        fail "Host port ${FRONTEND_PORT} is already in use! Choose a free FRONTEND_HOST_PORT in .env"
    else
        pass "Host port ${FRONTEND_PORT} is free for asc-frontend (127.0.0.1:${FRONTEND_PORT})"
    fi

    if printf "%s\n" "${LISTENERS}" | grep -qE ':80[[:space:]]'; then
        info "Port 80 is listening (expected for existing host Nginx)"
    fi
    if printf "%s\n" "${LISTENERS}" | grep -qE ':8080[[:space:]]'; then
        info "Port 8080 is occupied (expected for skillswap-backend; Smart Otter will NOT bind 8080)"
    fi
else
    warn "'ss' command not found; verify port ${FRONTEND_PORT} manually"
fi

# 4. Check Docker Resource Name Collisions & Coexistence
if docker info >/dev/null 2>&1; then
    ALL_CONTAINERS=$(docker ps -a --format '{{.Names}}|{{.Label "com.docker.compose.project"}}|{{.Status}}' 2>/dev/null || true)
    for cname in asc-postgres asc-backend asc-frontend; do
        match=$(printf "%s\n" "${ALL_CONTAINERS}" | grep -E "^${cname}\|" || true)
        if [ -z "${match}" ]; then
            pass "Container name '${cname}' is unclaimed"
        else
            proj=$(printf "%s" "${match}" | cut -d'|' -f2)
            if [ "${proj}" = "smart-otter" ]; then
                info "Container '${cname}' already belongs to project 'smart-otter'"
            else
                fail "Container '${cname}' exists under project '${proj}' (collision)"
            fi
        fi
    done

    SKILLSWAP_RUNNING=$(docker ps --format '{{.Names}} ({{.Status}})' 2>/dev/null | grep -E 'skillswap' || true)
    if [ -n "${SKILLSWAP_RUNNING}" ]; then
        info "Detected running legacy stack (will remain untouched):"
        printf "%s\n" "${SKILLSWAP_RUNNING}" | while IFS= read -r line; do
            info "  - ${line}"
        done
    fi

    NET_MATCH=$(docker network ls --format '{{.Name}}' 2>/dev/null | grep -E '^asc_network$' || true)
    if [ -z "${NET_MATCH}" ]; then
        pass "Isolated Docker network 'asc_network' is free to create"
    else
        info "Docker network 'asc_network' already exists"
    fi
fi

# 5. Check Nginx syntax (read-only)
if command -v nginx >/dev/null 2>&1; then
    if [ "$(id -u)" -eq 0 ]; then
        if nginx -t >/dev/null 2>&1; then
            pass "Existing Nginx configuration passes 'nginx -t'"
        else
            warn "Existing Nginx configuration returned non-zero on 'nginx -t'; inspect before Phase 3"
        fi
    else
        info "Run 'sudo nginx -t' to verify existing Nginx config before Phase 3"
    fi
fi

# 6. Check Smart Otter repo & .env readiness (no secret values printed!)
if [ -f docker-compose.yml ] && [ -f .env.example ]; then
    pass "Found Smart Otter workspace files (docker-compose.yml, .env.example)"
    if [ -f .env ]; then
        PERM=$(stat -c '%a' .env 2>/dev/null || echo 'unknown')
        if [ "${PERM}" = "600" ] || [ "${PERM}" = "400" ]; then
            pass ".env file exists with strict permission (${PERM})"
        else
            warn ".env exists with permission ${PERM}; run 'chmod 600 .env'"
        fi

        check_env_set() {
            local key="$1"
            local val
            val=$(grep -E "^${key}=" .env | tail -n1 | cut -d'=' -f2- | tr -d '\r' || true)
            if [ -z "${val}" ] || printf "%s" "${val}" | grep -qE '^change_me'; then
                fail ".env '${key}' is missing or still uses placeholder"
            else
                pass ".env '${key}' is configured"
            fi
        }

        check_env_set "POSTGRES_PASSWORD"
        check_env_set "JWT_SECRET"
        check_env_set "CKEY_API_KEY"
        check_env_set "BRAVE_API_KEY"

        JWT_VAL=$(grep -E '^JWT_SECRET=' .env | tail -n1 | cut -d'=' -f2- | tr -d '\r' || true)
        if [ "${#JWT_VAL}" -lt 32 ]; then
            fail ".env 'JWT_SECRET' must be at least 32 characters"
        fi

        NEXT_API=$(grep -E '^NEXT_PUBLIC_API_URL=' .env | tail -n1 | cut -d'=' -f2- | tr -d '\r' || true)
        if [ -n "${NEXT_API}" ]; then
            warn ".env 'NEXT_PUBLIC_API_URL' is non-empty ('${NEXT_API}'); PA1 recommends leaving it empty for /api/* proxy"
        else
            pass ".env 'NEXT_PUBLIC_API_URL' is empty (PA1 proxy mode)"
        fi

        if docker compose -p smart-otter config --quiet >/dev/null 2>&1; then
            pass "'docker compose -p smart-otter config --quiet' is valid"
        else
            fail "'docker compose -p smart-otter config' failed"
        fi
    else
        info ".env not created yet (copy .env.example -> .env as part of Phase 1/2)"
    fi
fi

printf "\n==========================================================\n"
printf " Summary: PASS=%d | WARN=%d | FAIL=%d\n" "${PASS_COUNT}" "${WARN_COUNT}" "${FAIL_COUNT}"
printf "==========================================================\n"

if [ "${FAIL_COUNT}" -gt 0 ]; then
    exit 1
fi
exit 0
