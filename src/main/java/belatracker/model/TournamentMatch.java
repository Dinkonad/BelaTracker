package belatracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tournament_matches")
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    private String phase;
    private int roundNo;
    private int slot;
    private int groupNo = -1;
    private boolean bye = false;
    private int winnerSide = 0;

    private Integer scoreA;
    private Integer scoreB;

    @ManyToOne private TournamentTeam teamA;
    @ManyToOne private TournamentTeam teamB;

    @ManyToOne private Match match;

    public boolean isDecided() { return winnerSide != 0; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament t) { this.tournament = t; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public int getRoundNo() { return roundNo; }
    public void setRoundNo(int r) { this.roundNo = r; }
    public int getSlot() { return slot; }
    public void setSlot(int s) { this.slot = s; }
    public int getGroupNo() { return groupNo; }
    public void setGroupNo(int g) { this.groupNo = g; }
    public boolean isBye() { return bye; }
    public void setBye(boolean b) { this.bye = b; }
    public int getWinnerSide() { return winnerSide; }
    public void setWinnerSide(int w) { this.winnerSide = w; }
    public int getScoreA() { return scoreA == null ? 0 : scoreA; }
    public void setScoreA(Integer s) { this.scoreA = s; }
    public int getScoreB() { return scoreB == null ? 0 : scoreB; }
    public void setScoreB(Integer s) { this.scoreB = s; }
    public boolean isHasScore() { return scoreA != null && scoreB != null; }
    public TournamentTeam getTeamA() { return teamA; }
    public void setTeamA(TournamentTeam t) { this.teamA = t; }
    public TournamentTeam getTeamB() { return teamB; }
    public void setTeamB(TournamentTeam t) { this.teamB = t; }
    public Match getMatch() { return match; }
    public void setMatch(Match m) { this.match = m; }

    public String getLabelA() {
        return teamA != null ? teamA.getLabel() : "—";
    }
    public String getLabelB() {
        return teamB != null ? teamB.getLabel() : "—";
    }

    public boolean isReady() {
        return teamA != null && teamB != null && !bye;
    }
}
