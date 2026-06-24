package belatracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "team1_player1_id")
    private player team1Player1;

    @ManyToOne
    @JoinColumn(name = "team1_player2_id")
    private player team1Player2;

    @ManyToOne
    @JoinColumn(name = "team2_player1_id")
    private player team2Player1;

    @ManyToOne
    @JoinColumn(name = "team2_player2_id")
    private player team2Player2;

    private int team1Score;
    private int team2Score;
    private int winner;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public player getTeam1Player1() { return team1Player1; }
    public void setTeam1Player1(player p) { this.team1Player1 = p; }

    public player getTeam1Player2() { return team1Player2; }
    public void setTeam1Player2(player p) { this.team1Player2 = p; }

    public player getTeam2Player1() { return team2Player1; }
    public void setTeam2Player1(player p) { this.team2Player1 = p; }

    public player getTeam2Player2() { return team2Player2; }
    public void setTeam2Player2(player p) { this.team2Player2 = p; }

    public int getTeam1Score() { return team1Score; }
    public void setTeam1Score(int s) { this.team1Score = s; }

    public int getTeam2Score() { return team2Score; }
    public void setTeam2Score(int s) { this.team2Score = s; }

    public int getWinner() { return winner; }
    public void setWinner(int w) { this.winner = w; }
}