/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom.cli;

import epox.webaom.Job;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Per-file result shared by human and structured CLI output. */
public final class CliFileResult {
    public enum Status {
        IDENTIFIED,
        UNKNOWN,
        HASHED,
        RENAME_PLANNED,
        MOVE_PLANNED,
        RENAMED,
        MOVED,
        UNCHANGED,
        MYLIST_ADDED,
        MYLIST_REMOVED,
        MYLIST_PRESENT,
        MYLIST_ABSENT,
        FAILED
    }

    public final Path source;
    public Path target;
    public Status status;
    public String message;
    public Job job;
    public boolean applied;
    public boolean persistenceFailed;
    public Map<FileHasher.Algorithm, String> hashes = Map.of();
    public final List<CompanionMove> companionMoves = new ArrayList<>();

    public CliFileResult(Path source) {
        this.source = source;
    }

    public boolean failed() {
        return status == Status.FAILED || status == Status.UNKNOWN || persistenceFailed;
    }

    /** A subtitle or other companion file that follows the primary file's rename. */
    public static final class CompanionMove {
        public final Path source;
        public final Path target;
        public boolean applied;

        CompanionMove(Path source, Path target) {
            this.source = source;
            this.target = target;
        }
    }
}
