package belatracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    private int team1Score;
    private int team2Score;
    private int caller;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Match getMatch() { return match; }
    public void setMatch(Match m) { this.match = m; }

    public int getTeam1Score() { return team1Score; }
    public void setTeam1Score(int s) { this.team1Score = s; }

    public int getTeam2Score() { return team2Score; }
    public void setTeam2Score(int s) { this.team2Score = s; }

    public int getCaller() { return caller; }
    public void setCaller(int c) { this.caller = c; }
}
