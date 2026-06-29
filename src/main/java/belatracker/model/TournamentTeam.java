package belatracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tournament_teams")
public class TournamentTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne private Player player1;
    @ManyToOne private Player player2;

    private int groupNo = -1;

    @Transient public int played;
    @Transient public int wins;
    @Transient public int losses;
    @Transient public int pf;
    @Transient public int pa;

    public int getDiff() { return pf - pa; }

    public String getLabel() {
        String a = player1 != null ? player1.getName() : "?";
        String b = player2 != null ? player2.getName() : "?";
        return a + " / " + b;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament t) { this.tournament = t; }
    public Player getPlayer1() { return player1; }
    public void setPlayer1(Player p) { this.player1 = p; }
    public Player getPlayer2() { return player2; }
    public void setPlayer2(Player p) { this.player2 = p; }
    public int getGroupNo() { return groupNo; }
    public void setGroupNo(int g) { this.groupNo = g; }
}
