#!/usr/bin/env bash
set -euo pipefail

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Not a Git working tree; skipping hook installation."
    exit 0
fi

repo_root="$(git rev-parse --show-toplevel)"
# Linked worktrees share the main repository's hooks directory, so ask Git where it is.
hooks_dir="$(git rev-parse --git-path hooks)"
mkdir -p "${hooks_dir}"

hooks=("pre-commit" "pre-push")
updated_any=false

for hook in "${hooks[@]}"; do
    source_hook="${repo_root}/.githooks/${hook}"
    target_hook="${hooks_dir}/${hook}"

    if [[ ! -f "${source_hook}" ]]; then
        echo "Missing source hook: ${source_hook}" >&2
        exit 1
    fi

    # The shim finds the hook at run time, so one shim serves every worktree with that worktree's own hook.
    shim_content=$(
        cat <<EOF
#!/usr/bin/env bash
set -euo pipefail
exec "\$(git rev-parse --show-toplevel)/.githooks/${hook}" "\$@"
EOF
    )

    # Leave an up-to-date hook untouched.
    if [[ -x "${source_hook}" ]] && [[ -x "${target_hook}" ]] \
        && [[ "$(cat "${target_hook}")" == "${shim_content}" ]]; then
        continue
    fi

    printf '%s\n' "${shim_content}" >"${target_hook}"
    chmod +x "${source_hook}" "${target_hook}"
    echo "Installed ${target_hook} -> .githooks/${hook}"
    updated_any=true
done

if [[ "${updated_any}" == true ]]; then
    echo "Git hooks installed."
fi
