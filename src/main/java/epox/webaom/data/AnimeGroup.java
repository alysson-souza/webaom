/*
 * Created on 27.feb.2006 12:44:44
 * Filename: aAGroup.java
 */
package epox.webaom.data;

/**
 * AG (Anime-Group) represents the association between an Anime and a Group (fansub/release group).
 * Used for organizing files by which group released them.
 */
public class AnimeGroup extends AniDBEntity {
    private final Group group;
    private final Anime anime;

    public AnimeGroup(Anime anime, Group group) {
        this.id = group.id;
        this.group = group;
        this.anime = anime;
    }

    /*
     * public Object getKey(){
     * return group.getKey();
     * }
     */
    public String toString() {
        return group.name + " (" + group.shortName + ")";
    }

    /**
     * Calculate completion percentage for this group's releases.
     *
     * @return percentage of episodes available from this group
     */
    public int getCompletionPercent() {
        int maxEpisodes = anime.episodeCount;
        if (maxEpisodes == 0) {
            maxEpisodes = -anime.latestEpisode;
        }
        if (maxEpisodes == 0) {
            return 0;
        }
        return (size() * 100) / maxEpisodes;
    }
}
