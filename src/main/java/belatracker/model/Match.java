package belatracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private String dealer;

    private int targetScore = 1001;
    private boolean finished = false;
    private int winner = 0;

    @ManyToOne @JoinColumn(name = "team1_player1_id")
    private Player team1Player1;
    @ManyToOne @JoinColumn(name = "team1_player2_id")
    private Player team1Player2;
    @ManyToOne @JoinColumn(name = "team2_player1_id")
    private Player team2Player1;
    @ManyToOne @JoinColumn(name = "team2_player2_id")
    private Player team2Player2;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Round> rounds = new ArrayList<>();
    public int getTeam1Total() {
        return rounds.stream().mapToInt(Round::getTeam1Score).sum();
    }
    public int getTeam2Total() {
        return rounds.stream().mapToInt(Round::getTeam2Score).sum();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDealer() { return dealer; }
    public void setDealer(String d) { this.dealer = d; }

    public int getTargetScore() { return targetScore; }
    public void setTargetScore(int targetScore) { this.targetScore = targetScore; }

    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }

    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }

    public Player getTeam1Player1() { return team1Player1; }
    public void setTeam1Player1(Player p) { this.team1Player1 = p; }
    public Player getTeam1Player2() { return team1Player2; }
    public void setTeam1Player2(Player p) { this.team1Player2 = p; }
    public Player getTeam2Player1() { return team2Player1; }
    public void setTeam2Player1(Player p) { this.team2Player1 = p; }
    public Player getTeam2Player2() { return team2Player2; }
    public void setTeam2Player2(Player p) { this.team2Player2 = p; }

    public List<Round> getRounds() { return rounds; }
    public void setRounds(List<Round> r) { this.rounds = r; }
}