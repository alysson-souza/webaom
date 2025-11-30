/*
 * Created on 28.feb.2006 19:11:21
 * Filename: Path.java
 */
package epox.webaom.data;

/**
 * Represents a file system path in the data tree.
 * Used for organizing files by their parent directory.
 */
public class Path extends AniDBEntity {
    private final String pathString;

    public Path(String path) {
        pathString = path;
    }

    public Object getKey() {
        return pathString;
    }

    public String toString() {
        return pathString;
    }
}
