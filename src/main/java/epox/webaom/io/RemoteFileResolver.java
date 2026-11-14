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

package epox.webaom.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Converts dropped locations to files without requiring desktop services for local paths. */
public final class RemoteFileResolver {
    /** Boundary to the optional desktop mount service. */
    @FunctionalInterface
    public interface MountClient {
        File mount(URI location) throws IOException;
    }

    /** A run of lookups, such as loading saved jobs, that requests each unavailable share only once. */
    public final class Batch {
        private final Map<URI, IOException> unavailable = new HashMap<>();

        private Batch() {}

        /** Resolves like {@link RemoteFileResolver#resolve(URI)}, repeating an earlier mount failure of the share. */
        public File resolve(URI location) throws IOException {
            return RemoteFileResolver.this.resolve(location, unavailable);
        }
    }

    private final String osName;
    private final MountClient mountClient;
    private final Map<Path, URI> mounts = new ConcurrentHashMap<>();
    private final Map<URI, Path> shares = new ConcurrentHashMap<>();

    /** Creates an optional KIO Fuse adapter; no desktop process is started until an SMB URL is resolved. */
    public RemoteFileResolver() {
        this(System.getProperty("os.name"), new KioFuseMountClient());
    }

    public RemoteFileResolver(String osName, MountClient mountClient) {
        this.osName = osName.toLowerCase(Locale.ROOT);
        this.mountClient = mountClient;
    }

    /** Resolves a location. Call from a background thread because remote mounts may require authentication. */
    public File resolve(URI location) throws IOException {
        return resolve(location, new HashMap<>());
    }

    /** Starts a batch for resolving many locations that may share an unavailable share. */
    public Batch batch() {
        return new Batch();
    }

    private File resolve(URI location, Map<URI, IOException> unavailable) throws IOException {
        try {
            if ("file".equalsIgnoreCase(location.getScheme())) {
                if ("localhost".equalsIgnoreCase(location.getAuthority())) {
                    location = new URI("file", null, location.getPath(), null, null);
                }
                if (osName.startsWith("windows")
                        && location.getAuthority() != null
                        && location.getQuery() == null
                        && location.getFragment() == null) {
                    return new File("//" + location.getAuthority() + location.getPath());
                }
                return new File(location);
            }
            if (!"smb".equalsIgnoreCase(location.getScheme())) {
                throw new IOException("This type of file URL is not supported. Open its mounted folder instead.");
            }
            if (!osName.equals("linux")) {
                throw new IOException(
                        "SMB URL drops require KIO Fuse on Linux. Mount the share and drop files from its local folder"
                                + " instead.");
            }
            List<String> segments = smbSegments(location);
            URI root = new URI("smb", location.getAuthority(), "/" + segments.get(0), null, null);
            // Request the share root, so directory scans and later renames retain their remote identity.
            Path file = mount(root, unavailable);
            for (String segment : segments.subList(1, segments.size())) {
                file = file.resolve(segment);
            }
            return file.toFile();
        } catch (IllegalArgumentException | URISyntaxException e) {
            throw new IOException("The drop contains an invalid file URL.");
        }
    }

    private Path mount(URI root, Map<URI, IOException> unavailable) throws IOException {
        Path known = shares.get(root);
        // Each request starts a qdbus process and can wait on the network, so reuse a mount while its folder exists.
        if (known != null && Files.isDirectory(known)) return known;
        IOException earlier = unavailable.get(root);
        if (earlier != null) throw earlier;
        try {
            File mounted = mountClient.mount(root);
            if (mounted == null || !mounted.isAbsolute()) {
                throw new IOException("KIO Fuse did not return an absolute mount path.");
            }
            Path mount = mounted.toPath().normalize();
            shares.put(root, mount);
            mounts.put(mount, root);
            return mount;
        } catch (IOException e) {
            unavailable.put(root, e);
            throw e;
        }
    }

    /** Returns a stable SMB URL for a file inside a share resolved by this instance, otherwise null. */
    public URI remoteUri(File file) {
        if (mounts.isEmpty()) return null;
        Path path;
        try {
            path = file.toPath().toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            // A path saved on another platform can hold characters this one rejects; it is not on a mount.
            return null;
        }
        return mounts.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .max(Comparator.comparingInt(entry -> entry.getKey().getNameCount()))
                .map(entry -> {
                    String relative = entry.getKey().relativize(path).toString().replace(File.separatorChar, '/');
                    URI root = entry.getValue();
                    try {
                        return new URI(
                                "smb",
                                root.getAuthority(),
                                root.getPath() + (relative.isEmpty() ? "" : "/" + relative),
                                null,
                                null);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .orElse(null);
    }

    /** Returns the decoded path segments of a valid SMB URL, share name first. */
    private static List<String> smbSegments(URI uri) throws IOException {
        // Read the authority itself: URI reports no host or user for server names it rejects, such as "my_nas".
        String authority = uri.getAuthority();
        if (authority == null
                || authority.endsWith("@")
                || uri.getPath().length() < 2
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IOException("An SMB URL must include a server and share name.");
        }
        int userEnd = authority.lastIndexOf('@');
        if (userEnd >= 0 && authority.substring(0, userEnd).contains(":")) {
            throw new IOException("Use an SMB URL without a password. Sign in through KDE when prompted.");
        }
        List<String> segments = new ArrayList<>();
        for (String raw : uri.getRawPath().substring(1).split("/", -1)) {
            String segment = URI.create("/" + raw).getPath().substring(1);
            if (segment.equals(".")
                    || segment.equals("..")
                    || segment.contains("/")
                    || segment.contains("\\")
                    || segment.indexOf('\0') >= 0) {
                throw new IOException("The SMB URL contains an invalid path segment.");
            }
            segments.add(segment);
        }
        if (segments.get(0).isEmpty()) throw new IOException("An SMB URL must include a share name.");
        return segments;
    }
}
