/*
 * Created on 23.mai.2006 14:52:46
 * Filename: VideoTrack.java
 */
package epox.av;

public class VideoTrack extends GenericTrack {
	public int pixel_width;
	public int pixel_height;
	public int display_width;
	public int display_height;
	public int fps;
	public boolean vfr;

	public VideoTrack(GenericTrack gt) {
		super(gt);
	}
}
