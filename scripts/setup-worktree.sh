#!/usr/bin/env bash
# Prepares a fresh checkout or worktree so the first build, test run, and commit work without further setup.
# Safe to run again at any time.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

fail() {
    echo "setup-worktree: $*" >&2
    exit 1
}

step() {
    echo
    echo "==> $*"
}

step "Gradle and the Java 21 toolchain"
# The wrapper downloads Gradle, and Gradle downloads a Java 21 toolchain when none is installed.
./gradlew --version || fail "Gradle did not start. Check network access and that a JDK is on PATH."

step "Git hooks"
./gradlew --quiet installGitHooks
hooks_dir="$(git rev-parse --git-path hooks)"
for hook in pre-commit pre-push; do
    [[ -x "${hooks_dir}/${hook}" ]] || fail "${hooks_dir}/${hook} is missing or not executable."
    [[ -x ".githooks/${hook}" ]] || fail ".githooks/${hook} is missing or not executable."
done
echo "pre-commit and pre-push run from ${hooks_dir}"

step "Dependencies, formatter, and compiled classes"
./gradlew resolveDependencies spotlessCheck testClasses \
    || fail "The build failed. Fix the error above, then run scripts/setup-worktree.sh again."

step "Offline check"
# Proves nothing is left to download, so later builds do not depend on the network.
./gradlew --offline --quiet spotlessCheck testClasses \
    || fail "The build still needs the network. Run scripts/setup-worktree.sh again while online."

cat <<'EOF'

Ready. Main and test classes are compiled, dependencies are cached, and the Git hooks are installed.
  ./gradlew test             non-UI tests
  ./gradlew testUi           UI tests (headless)
  ./gradlew spotlessApply    format before committing
EOF
