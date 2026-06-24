package belatracker.service;

import belatracker.model.Match;
import belatracker.model.player;
import belatracker.repository.MatchRepository;
import belatracker.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    public MatchService(MatchRepository matchRepository, PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAllByOrderByDateDesc();
    }

    public Match getMatchById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));
    }

    @Transactional
    public void saveMatch(Match match) {

         player p1 = match.getTeam1Player1();
        player p2 = match.getTeam1Player2();
        player p3 = match.getTeam2Player1();
        player p4 = match.getTeam2Player2();

        if (p1 == null || p2 == null || p3 == null || p4 == null) {
            throw new IllegalArgumentException("All players must be selected.");
        }

        matchRepository.save(match);

        if (match.getWinner() == 1) {
            p1.setWins(p1.getWins() + 1);
            p2.setWins(p2.getWins() + 1);

            p3.setLosses(p3.getLosses() + 1);
            p4.setLosses(p4.getLosses() + 1);

        } else if (match.getWinner() == 2) {
            p3.setWins(p3.getWins() + 1);
            p4.setWins(p4.getWins() + 1);

            p1.setLosses(p1.getLosses() + 1);
            p2.setLosses(p2.getLosses() + 1);
        }

        playerRepository.saveAll(List.of(p1, p2, p3, p4));
    }

    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }
}