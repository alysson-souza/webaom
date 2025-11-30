/*
 * Created on 23.mai.2006 14:52:46
 * Filename: VideoTrack.java
 */
package epox.av;

/**
 * Video track metadata extracted from media files.
 * Fields are populated by native JNI code via AVInfo.trackGetVideo().
 *
 * <p>
 * Note: Field names use snake_case to match JNI native library expectations.
 * Do not rename without updating the native avinfo library.
 */
public class VideoTrack extends GenericTrack {
	/** Video width in pixels (native resolution). */
	public int pixel_width;

	/** Video height in pixels (native resolution). */
	public int pixel_height;

	/** Display width (after aspect ratio adjustment). */
	public int display_width;

	/** Display height (after aspect ratio adjustment). */
	public int display_height;

	/** Frames per second (multiplied by 1000 for precision). */
	public int fps;

	/** Whether the video uses variable frame rate. */
	public boolean vfr;

	public VideoTrack(GenericTrack gt) {
		super(gt);
	}
}
