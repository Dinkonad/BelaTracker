package belatracker.service;

import belatracker.model.Match;
import belatracker.model.Player;
import belatracker.repository.MatchRepository;
import belatracker.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    public StatisticsService(MatchRepository matchRepository, PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    // DTO za statistiku para
    public static class PairStats {
        public String player1Name;
        public String player2Name;
        public int wins;
        public int losses;
        public int played;
        public double winPct;
        public int totalPointsFor;
        public int totalPointsAgainst;

        public String getWinPctStr() {
            return played == 0 ? "—" : String.format("%.0f%%", winPct);
        }
    }

    // DTO za head-to-head između dva para
    public static class H2HEntry {
        public String pairA;
        public String pairB;
        public int winsA;
        public int winsB;
        public int played;
    }

    // DTO za "best scorer" igrač
    public static class PlayerDetailStats {
        public String name;
        public String nickname;
        public int wins;
        public int losses;
        public int played;
        public double winPct;
        public int totalPointsFor;    // ukupno bodova koje su njegovi timovi skupili
        public int totalPointsAgainst;
        public double avgPointsPerMatch;
        public int longestWinStreak;
        public int currentStreak; // + = win streak, - = loss streak
        public String currentStreakLabel;
        public int tournamentWins;

        public String getWinPctStr() {
            return played == 0 ? "—" : String.format("%.0f%%", winPct);
        }
    }

    private String pairKey(Long id1, Long id2) {
        long a = Math.min(id1, id2);
        long b = Math.max(id1, id2);
        return a + "_" + b;
    }

    private String pairName(Player p1, Player p2) {
        return displayName(p1) + " & " + displayName(p2);
    }

    private String displayName(Player p) {
        if (p == null) return "?";
        if (p.getNickname() != null && !p.getNickname().isBlank())
            return p.getName() + " (" + p.getNickname() + ")";
        return p.getName();
    }

    public List<PairStats> getPairLeaderboard() {
        List<Match> matches = matchRepository.findAllByOrderByDateDescIdDesc();
        Map<String, PairStats> map = new LinkedHashMap<>();

        for (Match m : matches) {
            if (!m.isFinished()) continue;
            Player p1a = m.getTeam1Player1(), p1b = m.getTeam1Player2();
            Player p2a = m.getTeam2Player1(), p2b = m.getTeam2Player2();
            if (p1a == null || p1b == null || p2a == null || p2b == null) continue;

            String k1 = pairKey(p1a.getId(), p1b.getId());
            String k2 = pairKey(p2a.getId(), p2b.getId());

            PairStats s1 = map.computeIfAbsent(k1, x -> {
                PairStats s = new PairStats();
                s.player1Name = displayName(p1a); s.player2Name = displayName(p1b); return s;
            });
            PairStats s2 = map.computeIfAbsent(k2, x -> {
                PairStats s = new PairStats();
                s.player1Name = displayName(p2a); s.player2Name = displayName(p2b); return s;
            });

            int t1 = m.getTeam1Total(), t2 = m.getTeam2Total();
            s1.played++; s2.played++;
            s1.totalPointsFor += t1; s1.totalPointsAgainst += t2;
            s2.totalPointsFor += t2; s2.totalPointsAgainst += t1;

            if (m.getWinner() == 1) { s1.wins++; s2.losses++; }
            else { s2.wins++; s1.losses++; }
        }

        for (PairStats s : map.values()) {
            s.winPct = s.played == 0 ? 0 : s.wins * 100.0 / s.played;
        }

        return map.values().stream()
                .sorted(Comparator.comparingDouble((PairStats s) -> s.winPct).reversed()
                        .thenComparing(Comparator.comparingInt((PairStats s) -> s.wins).reversed()))
                .collect(Collectors.toList());
    }

    public List<H2HEntry> getHeadToHead() {
        List<Match> matches = matchRepository.findAllByOrderByDateDescIdDesc();
        // key = sorted pair keys combined
        Map<String, H2HEntry> map = new LinkedHashMap<>();

        for (Match m : matches) {
            if (!m.isFinished()) continue;
            Player p1a = m.getTeam1Player1(), p1b = m.getTeam1Player2();
            Player p2a = m.getTeam2Player1(), p2b = m.getTeam2Player2();
            if (p1a == null || p1b == null || p2a == null || p2b == null) continue;

            String k1 = pairKey(p1a.getId(), p1b.getId());
            String k2 = pairKey(p2a.getId(), p2b.getId());

            // combined key: always smaller first
            String combined = k1.compareTo(k2) <= 0 ? k1 + "|" + k2 : k2 + "|" + k1;
            boolean flipped = k1.compareTo(k2) > 0;

            H2HEntry e = map.computeIfAbsent(combined, x -> {
                H2HEntry h = new H2HEntry();
                if (!flipped) {
                    h.pairA = pairName(p1a, p1b);
                    h.pairB = pairName(p2a, p2b);
                } else {
                    h.pairA = pairName(p2a, p2b);
                    h.pairB = pairName(p1a, p1b);
                }
                return h;
            });

            e.played++;
            int winner = m.getWinner(); // 1 = team1, 2 = team2
            boolean team1IsA = !flipped;
            if ((winner == 1 && team1IsA) || (winner == 2 && !team1IsA)) e.winsA++;
            else e.winsB++;
        }

        return map.values().stream()
                .filter(e -> e.played >= 2)
                .sorted(Comparator.comparingInt((H2HEntry e) -> e.played).reversed())
                .collect(Collectors.toList());
    }

    public List<PlayerDetailStats> getDetailedPlayerStats() {
        List<Player> players = playerRepository.findAllByOrderByWinsDescLossesAsc();
        List<Match> allMatches = matchRepository.findAllByOrderByDateDescIdDesc();

        // sort matches oldest-first for streak calculation
        List<Match> chronological = new ArrayList<>(allMatches);
        Collections.reverse(chronological);

        Map<Long, PlayerDetailStats> map = new LinkedHashMap<>();
        for (Player p : players) {
            PlayerDetailStats ps = new PlayerDetailStats();
            ps.name = p.getName();
            ps.nickname = p.getNickname();
            ps.wins = p.getWins();
            ps.losses = p.getLosses();
            ps.played = p.getWins() + p.getLosses();
            ps.winPct = ps.played == 0 ? 0 : ps.wins * 100.0 / ps.played;
            ps.tournamentWins = p.getTournamentWins();
            map.put(p.getId(), ps);
        }

        for (Match m : allMatches) {
            if (!m.isFinished()) continue;
            Player[] t1 = {m.getTeam1Player1(), m.getTeam1Player2()};
            Player[] t2 = {m.getTeam2Player1(), m.getTeam2Player2()};
            int score1 = m.getTeam1Total(), score2 = m.getTeam2Total();

            for (Player p : t1) {
                if (p == null) continue;
                PlayerDetailStats ps = map.get(p.getId());
                if (ps == null) continue;
                ps.totalPointsFor += score1;
                ps.totalPointsAgainst += score2;
            }
            for (Player p : t2) {
                if (p == null) continue;
                PlayerDetailStats ps = map.get(p.getId());
                if (ps == null) continue;
                ps.totalPointsFor += score2;
                ps.totalPointsAgainst += score1;
            }
        }
        Map<Long, Integer> currentStreakMap = new HashMap<>();
        Map<Long, Integer> bestStreakMap = new HashMap<>();

        for (Match m : chronological) {
            if (!m.isFinished()) continue;
            Player[] winners = m.getWinner() == 1
                    ? new Player[]{m.getTeam1Player1(), m.getTeam1Player2()}
                    : new Player[]{m.getTeam2Player1(), m.getTeam2Player2()};
            Player[] losers = m.getWinner() == 1
                    ? new Player[]{m.getTeam2Player1(), m.getTeam2Player2()}
                    : new Player[]{m.getTeam1Player1(), m.getTeam1Player2()};

            for (Player p : winners) {
                if (p == null) continue;
                int streak = currentStreakMap.getOrDefault(p.getId(), 0);
                streak = streak < 0 ? 1 : streak + 1;
                currentStreakMap.put(p.getId(), streak);
                bestStreakMap.merge(p.getId(), streak, Math::max);
            }
            for (Player p : losers) {
                if (p == null) continue;
                int streak = currentStreakMap.getOrDefault(p.getId(), 0);
                streak = streak > 0 ? -1 : streak - 1;
                currentStreakMap.put(p.getId(), streak);
            }
        }

        for (Player p : players) {
            PlayerDetailStats ps = map.get(p.getId());
            if (ps == null) continue;
            ps.avgPointsPerMatch = ps.played == 0 ? 0 : (double) ps.totalPointsFor / ps.played;
            ps.longestWinStreak = bestStreakMap.getOrDefault(p.getId(), 0);
            int cs = currentStreakMap.getOrDefault(p.getId(), 0);
            ps.currentStreak = cs;
            if (cs > 0) ps.currentStreakLabel = cs + "P 🔥";
            else if (cs < 0) ps.currentStreakLabel = Math.abs(cs) + "P ❌";
            else ps.currentStreakLabel = "—";
        }

        return new ArrayList<>(map.values());
    }
}
