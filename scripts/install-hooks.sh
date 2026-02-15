#!/usr/bin/env bash
set -euo pipefail

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Not a Git working tree; skipping hook installation."
    exit 0
fi

repo_root="$(git rev-parse --show-toplevel)"
git_dir="$(git rev-parse --git-dir)"

hooks=("pre-commit" "pre-push")
updated_any=false

for hook in "${hooks[@]}"; do
    source_hook="${repo_root}/.githooks/${hook}"
    target_hook="${git_dir}/hooks/${hook}"

    if [[ ! -f "${source_hook}" ]]; then
        echo "Missing source hook: ${source_hook}" >&2
        exit 1
    fi

    shim_content=$(
        cat <<EOF
#!/usr/bin/env bash
set -euo pipefail
exec "${source_hook}" "\$@"
EOF
    )

    if [[ -f "${target_hook}" ]] && [[ "$(cat "${target_hook}")" == "${shim_content}" ]]; then
        chmod +x "${source_hook}" "${target_hook}"
        continue
    fi

    printf '%s\n' "${shim_content}" >"${target_hook}"
    chmod +x "${source_hook}" "${target_hook}"
    echo "Installed ${target_hook} -> ${source_hook}"
    updated_any=true
done

if [[ "${updated_any}" == true ]]; then
    echo "Git hooks installed."
fi
