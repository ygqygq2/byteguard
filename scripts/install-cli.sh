#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly DEFAULT_PREFIX="${HOME}/.local"
readonly APP_NAME="byteguard"
readonly DIST_TASK=":byteguard-cli:installDist"
readonly DIST_DIR="${PROJECT_ROOT}/byteguard-cli/build/install/byteguard-cli"

function Log_Info() {
  echo "[INFO] $1"
}

function Log_Error() {
  echo "[ERROR] $1" >&2
}

function Print_Usage() {
  cat <<'USAGE'
Usage: ./scripts/install-cli.sh [options]

Options:
  --prefix <path>   Install prefix (default: ~/.local)
  --system          Install to /usr/local
  --skip-build      Reuse existing build/install output
  --help            Show this help message

Examples:
  ./scripts/install-cli.sh
  ./scripts/install-cli.sh --prefix /tmp/byteguard-install
  sudo ./scripts/install-cli.sh --system
USAGE
}

function Ensure_Writable_Directory() {
  local dir_path="$1"

  mkdir -p "${dir_path}"
  if [[ ! -w "${dir_path}" ]]; then
    Log_Error "Directory is not writable: ${dir_path}"
    Log_Error "Try using sudo for a system install, or provide a user-writable --prefix."
    exit 1
  fi
}

function Build_Distribution() {
  Log_Info "Building ByteGuard CLI distribution..."
  cd "${PROJECT_ROOT}"
  ./gradlew "${DIST_TASK}"
}

function Install_Distribution() {
  local prefix="$1"
  local lib_dir="${prefix}/lib"
  local bin_dir="${prefix}/bin"
  local target_dir="${lib_dir}/byteguard-cli"

  Ensure_Writable_Directory "${lib_dir}"
  Ensure_Writable_Directory "${bin_dir}"

  if [[ ! -d "${DIST_DIR}" ]]; then
    Log_Error "Distribution directory not found: ${DIST_DIR}"
    Log_Error "Run the script without --skip-build, or build ${DIST_TASK} first."
    exit 1
  fi

  Log_Info "Installing CLI files to ${target_dir}"
  rm -rf "${target_dir}"
  mkdir -p "${target_dir}"
  cp -R "${DIST_DIR}/"* "${target_dir}/"

  ln -sfn "${target_dir}/bin/byteguard-cli" "${bin_dir}/${APP_NAME}"
  ln -sfn "${target_dir}/bin/byteguard-cli" "${bin_dir}/byteguard-cli"

  Log_Info "Install complete"
  Log_Info "Command path: ${bin_dir}/${APP_NAME}"

  if [[ ":${PATH}:" != *":${bin_dir}:"* ]]; then
    echo
    echo "Add this to your shell profile if needed:"
    echo "  export PATH=\"${bin_dir}:\$PATH\""
  fi
}

function Main() {
  local prefix="${DEFAULT_PREFIX}"
  local skip_build="false"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --prefix)
        prefix="$2"
        shift 2
        ;;
      --system)
        prefix="/usr/local"
        shift
        ;;
      --skip-build)
        skip_build="true"
        shift
        ;;
      --help)
        Print_Usage
        return 0
        ;;
      *)
        Log_Error "Unknown option: $1"
        Print_Usage
        return 1
        ;;
    esac
  done

  if [[ "${skip_build}" != "true" ]]; then
    Build_Distribution
  fi

  Install_Distribution "${prefix}"
}

Main "$@"
