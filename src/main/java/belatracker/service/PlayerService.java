package belatracker.service;

import belatracker.model.Match;
import belatracker.model.Player;
import belatracker.repository.MatchRepository;
import belatracker.repository.PlayerRepository;
import belatracker.repository.TournamentMatchRepository;
import belatracker.repository.TournamentTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TournamentMatchRepository tournamentMatchRepository;

    public PlayerService(PlayerRepository playerRepository,
                         MatchRepository matchRepository,
                         TournamentTeamRepository tournamentTeamRepository,
                         TournamentMatchRepository tournamentMatchRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.tournamentMatchRepository = tournamentMatchRepository;
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public List<Player> getLeaderboard() {
        return playerRepository.findAllByOrderByWinsDescLossesAsc();
    }

    public Player getPlayerById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Igrač nije pronađen, id: " + id));
    }

    public void savePlayer(Player player) {
        playerRepository.save(player);
    }

    @Transactional
    public void savePlayerPreserveStats(Player player) {
        if (player.getId() != null) {
            Player existing = playerRepository.findById(player.getId()).orElse(null);
            if (existing != null) {
                existing.setName(player.getName());
                existing.setNickname(player.getNickname());
                playerRepository.save(existing);
                return;
            }
        }
        playerRepository.save(player);
    }

    @Transactional
    public void deletePlayer(Long id) {
        List<Match> matches = matchRepository.findAllByOrderByDateDescIdDesc();
        for (Match m : matches) {
            boolean changed = false;
            if (m.getTeam1Player1() != null && m.getTeam1Player1().getId().equals(id)) {
                m.setTeam1Player1(null); changed = true;
            }
            if (m.getTeam1Player2() != null && m.getTeam1Player2().getId().equals(id)) {
                m.setTeam1Player2(null); changed = true;
            }
            if (m.getTeam2Player1() != null && m.getTeam2Player1().getId().equals(id)) {
                m.setTeam2Player1(null); changed = true;
            }
            if (m.getTeam2Player2() != null && m.getTeam2Player2().getId().equals(id)) {
                m.setTeam2Player2(null); changed = true;
            }
            if (changed) matchRepository.save(m);
        }

        tournamentMatchRepository.findAll().forEach(tm -> {
            boolean changed = false;
            if (tm.getA1() != null && tm.getA1().getId().equals(id)) { tm.setA1(null); changed = true; }
            if (tm.getA2() != null && tm.getA2().getId().equals(id)) { tm.setA2(null); changed = true; }
            if (tm.getB1() != null && tm.getB1().getId().equals(id)) { tm.setB1(null); changed = true; }
            if (tm.getB2() != null && tm.getB2().getId().equals(id)) { tm.setB2(null); changed = true; }
            if (changed) tournamentMatchRepository.save(tm);
        });

        tournamentTeamRepository.findAll().forEach(tt -> {
            boolean changed = false;
            if (tt.getPlayer1() != null && tt.getPlayer1().getId().equals(id)) { tt.setPlayer1(null); changed = true; }
            if (tt.getPlayer2() != null && tt.getPlayer2().getId().equals(id)) { tt.setPlayer2(null); changed = true; }
            if (changed) tournamentTeamRepository.save(tt);
        });

        playerRepository.deleteById(id);
    }

    public List<Player> searchPlayers(String name) {
        return playerRepository.findByNameContainingIgnoreCase(name);
    }
}
