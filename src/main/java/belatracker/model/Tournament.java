package belatracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private TournamentFormat format;

    private int targetScore = 701;
    private LocalDate date;

    private String status = "IN_PROGRESS";
    private String champion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TournamentFormat getFormat() { return format; }
    public void setFormat(TournamentFormat format) { this.format = format; }
    public int getTargetScore() { return targetScore; }
    public void setTargetScore(int targetScore) { this.targetScore = targetScore; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChampion() { return champion; }
    public void setChampion(String champion) { this.champion = champion; }

    public boolean isFinished() { return "FINISHED".equals(status); }
}
