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
    private int trick1;
    private int trick2;
    private int decl1;
    private int decl2;
    private int stiglja;

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

    public int getTrick1() { return trick1; }
    public void setTrick1(int t) { this.trick1 = t; }

    public int getTrick2() { return trick2; }
    public void setTrick2(int t) { this.trick2 = t; }

    public int getDecl1() { return decl1; }
    public void setDecl1(int d) { this.decl1 = d; }

    public int getDecl2() { return decl2; }
    public void setDecl2(int d) { this.decl2 = d; }

    public int getStiglja() { return stiglja; }
    public void setStiglja(int s) { this.stiglja = s; }
}
