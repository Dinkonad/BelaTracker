package belatracker.service;

import belatracker.model.Match;
import belatracker.model.Player;
import belatracker.model.Round;
import belatracker.repository.MatchRepository;
import belatracker.repository.PlayerRepository;
import belatracker.repository.RoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;

    public MatchService(MatchRepository matchRepository,
                        PlayerRepository playerRepository,
                        RoundRepository roundRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAllByOrderByDateDescIdDesc();
    }

    public Match getMatchById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partija nije pronađena, id: " + id));
    }

    /** Kreiranje nove partije: provjeri igrače, postavi datum, spremi (bez rundi). */
    @Transactional
    public Match saveMatch(Match match) {
        Player p1 = match.getTeam1Player1();
        Player p2 = match.getTeam1Player2();
        Player p3 = match.getTeam2Player1();
        Player p4 = match.getTeam2Player2();

        if (p1 == null || p2 == null || p3 == null || p4 == null) {
            throw new IllegalArgumentException("Moraju biti odabrana sva 4 igrača.");
        }
        if (match.getDate() == null) {
            match.setDate(LocalDate.now());
        }
        return matchRepository.save(match);
    }

    @Transactional
    public void deleteMatch(Long id) {
        Match match = getMatchById(id);
        // ako je partija bila zaključena, vrati statistiku natrag prije brisanja
        if (match.isFinished() && match.getWinner() != 0) {
            applyResult(match, -1);
        }
        matchRepository.delete(match);
    }

    // ---------- RUNDE / DIJELJENJA ----------

    @Transactional
    public void addRound(Long matchId, Round round) {
        Match match = getMatchById(matchId);
        if (match.isFinished()) {
            throw new IllegalStateException("Partija je zaključena, nije moguće dodavati dijeljenja.");
        }
        round.setId(null);
        round.setMatch(match);
        roundRepository.save(round);
    }

    public Round getRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Dijeljenje nije pronađeno, id: " + roundId));
    }

    @Transactional
    public void updateRound(Round round) {
        Round existing = getRound(round.getId());
        existing.setTeam1Score(round.getTeam1Score());
        existing.setTeam2Score(round.getTeam2Score());
        existing.setCaller(round.getCaller());
        roundRepository.save(existing);
    }

    @Transactional
    public void deleteRound(Long roundId) {
        roundRepository.deleteById(roundId);
    }

    @Transactional
    public void setDealer(Long matchId, String dealer) {
        Match match = getMatchById(matchId);
        match.setDealer(dealer);
        matchRepository.save(match);
    }

    // ---------- ZAKLJUČIVANJE PARTIJE ----------

    /** Zaključi partiju: odredi pobjednika po ukupnom rezultatu i ažuriraj statistiku. */
    @Transactional
    public void finishMatch(Long id) {
        Match match = getMatchById(id);
        if (match.isFinished()) {
            return; // već zaključena, ne diraj statistiku ponovno
        }
        int t1 = match.getTeam1Total();
        int t2 = match.getTeam2Total();
        if (t1 == t2) {
            throw new IllegalStateException("Rezultat je izjednačen – partiju nije moguće zaključiti.");
        }
        match.setWinner(t1 > t2 ? 1 : 2);
        match.setFinished(true);
        applyResult(match, +1);
        matchRepository.save(match);
    }

    /** Dodaje (sign=+1) ili poništava (sign=-1) pobjede/poraze za sudionike partije. */
    private void applyResult(Match match, int sign) {
        int winner = match.getWinner();
        if (winner == 0) return;

        List<Player> winners = new ArrayList<>();
        List<Player> losers = new ArrayList<>();

        if (winner == 1) {
            addIfNotNull(winners, match.getTeam1Player1(), match.getTeam1Player2());
            addIfNotNull(losers, match.getTeam2Player1(), match.getTeam2Player2());
        } else {
            addIfNotNull(winners, match.getTeam2Player1(), match.getTeam2Player2());
            addIfNotNull(losers, match.getTeam1Player1(), match.getTeam1Player2());
        }

        for (Player p : winners) p.setWins(Math.max(0, p.getWins() + sign));
        for (Player p : losers) p.setLosses(Math.max(0, p.getLosses() + sign));

        List<Player> changed = new ArrayList<>();
        changed.addAll(winners);
        changed.addAll(losers);
        playerRepository.saveAll(changed);
    }

    private void addIfNotNull(List<Player> list, Player... players) {
        for (Player p : players) {
            if (p != null) list.add(p);
        }
    }
}