package belatracker.service;

import belatracker.model.*;
import belatracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepo;
    private final TournamentTeamRepository teamRepo;
    private final TournamentMatchRepository tmRepo;
    private final PlayerRepository playerRepo;
    private final MatchService matchService;

    public TournamentService(TournamentRepository tournamentRepo,
                             TournamentTeamRepository teamRepo,
                             TournamentMatchRepository tmRepo,
                             PlayerRepository playerRepo,
                             MatchService matchService) {
        this.tournamentRepo = tournamentRepo;
        this.teamRepo = teamRepo;
        this.tmRepo = tmRepo;
        this.playerRepo = playerRepo;
        this.matchService = matchService;
    }

    public static class Standing {
        public String name; public int played, wins, pf, pa;
        public Standing(String n){ this.name = n; }
        public String getName(){ return name; }
        public int getPlayed(){ return played; }
        public int getWins(){ return wins; }
        public int getPf(){ return pf; }
        public int getPa(){ return pa; }
        public int getDiff(){ return pf - pa; }
    }
    public record RoundView(int roundNo, String title, List<TournamentMatch> matches) {
        public int getRoundNo(){ return roundNo; }
        public String getTitle(){ return title; }
        public List<TournamentMatch> getMatches(){ return matches; }
    }
    public record GroupView(int groupNo, String title, List<TournamentTeam> standings, List<TournamentMatch> matches) {
        public int getGroupNo(){ return groupNo; }
        public String getTitle(){ return title; }
        public List<TournamentTeam> getStandings(){ return standings; }
        public List<TournamentMatch> getMatches(){ return matches; }
    }

    public List<Tournament> getAll() { return tournamentRepo.findAllByOrderByIdDesc(); }
    public Tournament get(Long id) {
        return tournamentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Turnir nije pronađen, id: " + id));
    }
    public List<TournamentMatch> matches(Long tid) {
        return tmRepo.findByTournamentIdOrderByRoundNoAscSlotAsc(tid);
    }
    public List<TournamentTeam> teams(Long tid) {
        return teamRepo.findByTournamentIdOrderByIdAsc(tid);
    }

    public TournamentMatch getTournamentMatch(Long tmId) {
        return tmRepo.findById(tmId)
                .orElseThrow(() -> new RuntimeException("Meč nije pronađen, id: " + tmId));
    }

    @Transactional
    public Tournament create(String name, TournamentFormat format, int target, List<Long> playerIds) {
        List<Player> players = new ArrayList<>(playerRepo.findAllById(playerIds));
        int n = players.size();

        if (format == TournamentFormat.AMERICAN) {
            if (n < 4 || n % 4 != 0)
                throw new IllegalArgumentException("Američki format treba broj igrača djeljiv s 4 (4, 8, 12...).");
        } else {
            if (n < 4 || n % 2 != 0)
                throw new IllegalArgumentException("Treba paran broj igrača (najmanje 4).");
            if (format == TournamentFormat.GROUPS_KNOCKOUT && n < 8)
                throw new IllegalArgumentException("Grupe + ždrijeb traže najmanje 8 igrača (4 tima).");
        }

        Collections.shuffle(players);

        Tournament t = new Tournament();
        t.setName((name == null || name.isBlank()) ? "Turnir" : name.trim());
        t.setFormat(format);
        t.setTargetScore(target);
        t.setDate(LocalDate.now());
        t.setStatus("IN_PROGRESS");
        t = tournamentRepo.save(t);

        switch (format) {
            case KNOCKOUT -> {
                List<TournamentTeam> teams = pairTeams(t, players, -1);
                buildKnockout(t, "KO", teams);
            }
            case LEAGUE -> {
                List<TournamentTeam> teams = pairTeams(t, players, -1);
                buildLeague(t, teams);
            }
            case GROUPS_KNOCKOUT -> {
                List<TournamentTeam> teams = pairTeams(t, players, -1);
                int g = Math.max(2, (int) Math.round(teams.size() / 4.0));
                buildGroupStage(t, teams, g);
            }
            case AMERICAN -> buildAmerican(t, players);
        }
        return t;
    }

    private List<TournamentTeam> pairTeams(Tournament t, List<Player> players, int group) {
        List<TournamentTeam> teams = new ArrayList<>();
        for (int i = 0; i + 1 < players.size(); i += 2) {
            TournamentTeam tt = new TournamentTeam();
            tt.setTournament(t);
            tt.setPlayer1(players.get(i));
            tt.setPlayer2(players.get(i + 1));
            tt.setGroupNo(group);
            teams.add(teamRepo.save(tt));
        }
        return teams;
    }

    private void buildKnockout(Tournament t, String phase, List<TournamentTeam> seeds) {
        int n = seeds.size();
        int bracket = 1; while (bracket < n) bracket *= 2;
        int rounds = 0; { int p = 1; while (p < bracket) { p *= 2; rounds++; } }

        List<TournamentMatch> all = new ArrayList<>();
        for (int r = 1; r <= rounds; r++) {
            int cnt = bracket >> r;
            for (int s = 0; s < cnt; s++) {
                TournamentMatch m = new TournamentMatch();
                m.setTournament(t); m.setPhase(phase); m.setRoundNo(r); m.setSlot(s);
                all.add(m);
            }
        }
        int byes = bracket - n;
        List<TournamentMatch> r1 = all.stream()
                .filter(m -> m.getRoundNo() == 1)
                .sorted(Comparator.comparingInt(TournamentMatch::getSlot)).toList();
        int idx = 0;
        for (int s = 0; s < r1.size(); s++) {
            TournamentMatch m = r1.get(s);
            if (s < byes) { m.setTeamA(seeds.get(idx++)); m.setBye(true); m.setWinnerSide(1); }
            else { m.setTeamA(seeds.get(idx++)); m.setTeamB(seeds.get(idx++)); }
        }
        tmRepo.saveAll(all);
    }

    private void buildLeague(Tournament t, List<TournamentTeam> teams) {
        int round = 1;
        for (int i = 0; i < teams.size(); i++)
            for (int j = i + 1; j < teams.size(); j++) {
                TournamentMatch m = new TournamentMatch();
                m.setTournament(t); m.setPhase("LEAGUE"); m.setRoundNo(round); m.setSlot(0);
                m.setTeamA(teams.get(i)); m.setTeamB(teams.get(j));
                tmRepo.save(m); round++;
            }
    }

    private void buildGroupStage(Tournament t, List<TournamentTeam> teams, int groups) {
        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).setGroupNo(i % groups);
            teamRepo.save(teams.get(i));
        }
        for (int g = 0; g < groups; g++) {
            List<TournamentTeam> grp = new ArrayList<>();
            for (TournamentTeam tt : teams) if (tt.getGroupNo() == g) grp.add(tt);
            int round = 1;
            for (int i = 0; i < grp.size(); i++)
                for (int j = i + 1; j < grp.size(); j++) {
                    TournamentMatch m = new TournamentMatch();
                    m.setTournament(t); m.setPhase("GROUP"); m.setGroupNo(g);
                    m.setRoundNo(round); m.setSlot(0);
                    m.setTeamA(grp.get(i)); m.setTeamB(grp.get(j));
                    tmRepo.save(m); round++;
                }
        }
    }

    private void buildAmerican(Tournament t, List<Player> players) {
        int rounds = Math.max(2, players.size() / 2);
        for (int r = 1; r <= rounds; r++) {
            List<Player> shuffled = new ArrayList<>(players);
            Collections.shuffle(shuffled);
            int table = 0;
            for (int i = 0; i + 3 < shuffled.size(); i += 4) {
                TournamentMatch m = new TournamentMatch();
                m.setTournament(t); m.setPhase("AMERICAN"); m.setRoundNo(r); m.setSlot(table++);
                m.setA1(shuffled.get(i));   m.setA2(shuffled.get(i + 1));
                m.setB1(shuffled.get(i + 2)); m.setB2(shuffled.get(i + 3));
                tmRepo.save(m);
            }
        }
    }
    @Transactional
    public Long playMatch(Long tmId) {
        TournamentMatch tm = tmRepo.findById(tmId)
                .orElseThrow(() -> new RuntimeException("Meč nije pronađen."));
        if (tm.getMatch() != null) return tm.getMatch().getId();
        if (!tm.isReady()) throw new IllegalStateException("Meč još nije spreman za igru.");

        Player a1, a2, b1, b2;
        if (tm.getTeamA() != null) {
            a1 = tm.getTeamA().getPlayer1(); a2 = tm.getTeamA().getPlayer2();
            b1 = tm.getTeamB().getPlayer1(); b2 = tm.getTeamB().getPlayer2();
        } else {
            a1 = tm.getA1(); a2 = tm.getA2(); b1 = tm.getB1(); b2 = tm.getB2();
        }

        Match m = new Match();
        m.setTargetScore(tm.getTournament().getTargetScore());
        m.setTeam1Player1(a1); m.setTeam1Player2(a2);
        m.setTeam2Player1(b1); m.setTeam2Player2(b2);
        m.setDealer(a1 != null ? a1.getName() : null);
        Match saved = matchService.saveMatch(m);

        tm.setMatch(saved);
        tmRepo.save(tm);
        return saved.getId();
    }

    @Transactional
    public void quickResult(Long tmId, int scoreA, int scoreB) {
        TournamentMatch tm = tmRepo.findById(tmId)
                .orElseThrow(() -> new RuntimeException("Meč nije pronađen."));
        if (!tm.isReady()) throw new IllegalStateException("Meč još nije spreman za unos.");
        if (scoreA == scoreB) throw new IllegalArgumentException("Rezultati ne smiju biti izjednačeni.");
        if (scoreA < 0 || scoreB < 0) throw new IllegalArgumentException("Bodovi ne mogu biti negativni.");

        tm.setScoreA(scoreA);
        tm.setScoreB(scoreB);
        tm.setWinnerSide(scoreA > scoreB ? 1 : 2);
        tmRepo.save(tm);
    }

    @Transactional
    public void sync(Long tid) {
        Tournament t = get(tid);
        if (t.isFinished()) return;

        for (TournamentMatch tm : matches(tid)) {
            if (tm.getWinnerSide() == 0 && tm.isBye()) {
                tm.setWinnerSide(tm.getTeamA() != null ? 1 : 2);
                tmRepo.save(tm);
            }
        }

        if (t.getFormat() == TournamentFormat.GROUPS_KNOCKOUT) {
            List<TournamentMatch> ko = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "KO");
            List<TournamentMatch> grp = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "GROUP");
            boolean allGroupDone = !grp.isEmpty() && grp.stream().allMatch(TournamentMatch::isDecided);
            if (ko.isEmpty() && allGroupDone) generateKnockoutFromGroups(t);
        }

        advanceKnockout(t);
        checkFinished(t);
    }

    private void advanceKnockout(Tournament t) {
        boolean changed = true;
        while (changed) {
            changed = false;
            List<TournamentMatch> ko = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(t.getId(), "KO");
            Map<String, TournamentMatch> byPos = new HashMap<>();
            for (TournamentMatch m : ko) byPos.put(m.getRoundNo() + "-" + m.getSlot(), m);

            for (TournamentMatch m : ko) {
                if (m.getWinnerSide() == 0) continue;
                TournamentTeam w = m.getWinnerSide() == 1 ? m.getTeamA() : m.getTeamB();
                if (w == null) continue;
                TournamentMatch next = byPos.get((m.getRoundNo() + 1) + "-" + (m.getSlot() / 2));
                if (next == null) continue;
                if (m.getSlot() % 2 == 0) {
                    if (next.getTeamA() == null) { next.setTeamA(w); tmRepo.save(next); changed = true; }
                } else {
                    if (next.getTeamB() == null) { next.setTeamB(w); tmRepo.save(next); changed = true; }
                }
            }
        }
    }

    private void generateKnockoutFromGroups(Tournament t) {
        List<GroupView> groups = buildGroups(t.getId());
        int g = groups.size();
        List<TournamentTeam> winners = new ArrayList<>();
        List<TournamentTeam> runners = new ArrayList<>();
        for (GroupView gv : groups) {
            winners.add(gv.standings().get(0));
            runners.add(gv.standings().size() > 1 ? gv.standings().get(1) : gv.standings().get(0));
        }
        List<TournamentTeam> seeds = new ArrayList<>();
        for (int i = 0; i < g; i++) {
            seeds.add(winners.get(i));
            seeds.add(runners.get((i + 1) % g));
        }
        buildKnockout(t, "KO", seeds);
    }

    private void checkFinished(Tournament t) {
        switch (t.getFormat()) {
            case KNOCKOUT, GROUPS_KNOCKOUT -> {
                List<TournamentMatch> ko = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(t.getId(), "KO");
                if (ko.isEmpty()) return;
                int maxRound = ko.stream().mapToInt(TournamentMatch::getRoundNo).max().orElse(0);
                TournamentMatch fin = ko.stream().filter(m -> m.getRoundNo() == maxRound).findFirst().orElse(null);
                if (fin != null && fin.isDecided()) {
                    TournamentTeam champ = fin.getWinnerSide() == 1 ? fin.getTeamA() : fin.getTeamB();
                    if (champ != null) finish(t, champ.getLabel(), List.of(champ.getPlayer1(), champ.getPlayer2()));
                }
            }
            case LEAGUE -> {
                List<TournamentMatch> ms = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(t.getId(), "LEAGUE");
                if (!ms.isEmpty() && ms.stream().allMatch(TournamentMatch::isDecided)) {
                    List<TournamentTeam> table = buildLeagueStandings(t.getId());
                    TournamentTeam champ = table.get(0);
                    finish(t, champ.getLabel(), List.of(champ.getPlayer1(), champ.getPlayer2()));
                }
            }
            case AMERICAN -> {
                List<TournamentMatch> ms = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(t.getId(), "AMERICAN");
                if (!ms.isEmpty() && ms.stream().allMatch(TournamentMatch::isDecided)) {
                    List<Standing> st = americanStandings(t.getId());
                    if (!st.isEmpty()) {
                        Standing top = st.get(0);
                        Player champP = playerRepo.findAll().stream()
                                .filter(p -> p.getName().equals(top.name)).findFirst().orElse(null);
                        finish(t, top.name, champP != null ? List.of(champP) : List.of());
                    }
                }
            }
        }
    }

    private void finish(Tournament t, String champion, List<Player> champions) {
        t.setStatus("FINISHED");
        t.setChampion(champion);
        tournamentRepo.save(t);
        for (Player p : champions) {
            if (p == null) continue;
            p.setTournamentWins(p.getTournamentWins() + 1);
            playerRepo.save(p);
        }
    }

    private void computeTeamStats(List<TournamentTeam> teams, List<TournamentMatch> ms) {
        Map<Long, TournamentTeam> byId = new HashMap<>();
        for (TournamentTeam t : teams) {
            t.played = t.wins = t.losses = t.pf = t.pa = 0;
            byId.put(t.getId(), t);
        }
        for (TournamentMatch m : ms) {
            if (m.getTeamA() == null || m.getTeamB() == null) continue;
            if (m.getWinnerSide() == 0 || !m.isHasScore()) continue;
            TournamentTeam A = byId.get(m.getTeamA().getId());
            TournamentTeam B = byId.get(m.getTeamB().getId());
            if (A == null || B == null) continue;
            int sa = m.getScoreA();
            int sb = m.getScoreB();
            A.played++; B.played++;
            A.pf += sa; A.pa += sb; B.pf += sb; B.pa += sa;
            if (m.getWinnerSide() == 1) { A.wins++; B.losses++; } else { B.wins++; A.losses++; }
        }
    }

    private Comparator<TournamentTeam> standingOrder() {
        return Comparator.comparingInt((TournamentTeam t) -> t.wins).reversed()
                .thenComparing(Comparator.comparingInt(TournamentTeam::getDiff).reversed());
    }

    @Transactional(readOnly = true)
    public List<TournamentTeam> buildLeagueStandings(Long tid) {
        List<TournamentTeam> teams = teams(tid);
        List<TournamentMatch> ms = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "LEAGUE");
        computeTeamStats(teams, ms);
        teams.sort(standingOrder());
        return teams;
    }

    @Transactional(readOnly = true)
    public List<TournamentMatch> leagueMatches(Long tid) {
        return tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "LEAGUE");
    }

    @Transactional(readOnly = true)
    public List<GroupView> buildGroups(Long tid) {
        List<TournamentTeam> teams = teams(tid);
        List<TournamentMatch> ms = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "GROUP");
        computeTeamStats(teams, ms);
        int groups = teams.stream().mapToInt(TournamentTeam::getGroupNo).max().orElse(-1) + 1;
        List<GroupView> out = new ArrayList<>();
        for (int g = 0; g < groups; g++) {
            final int gg = g;
            List<TournamentTeam> grp = teams.stream().filter(t -> t.getGroupNo() == gg)
                    .sorted(standingOrder()).collect(Collectors.toList());
            List<TournamentMatch> gm = ms.stream().filter(m -> m.getGroupNo() == gg).collect(Collectors.toList());
            out.add(new GroupView(g, "Grupa " + (char) ('A' + g), grp, gm));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<RoundView> knockoutRounds(Long tid) {
        List<TournamentMatch> ko = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "KO");
        Map<Integer, List<TournamentMatch>> byRound = new TreeMap<>();
        for (TournamentMatch m : ko) byRound.computeIfAbsent(m.getRoundNo(), k -> new ArrayList<>()).add(m);
        List<RoundView> out = new ArrayList<>();
        for (Map.Entry<Integer, List<TournamentMatch>> e : byRound.entrySet()) {
            out.add(new RoundView(e.getKey(), roundTitle(e.getValue().size()), e.getValue()));
        }
        return out;
    }

    private String roundTitle(int matchesInRound) {
        return switch (matchesInRound) {
            case 1 -> "Finale";
            case 2 -> "Polufinale";
            case 4 -> "Četvrtfinale";
            case 8 -> "Osmina finala";
            default -> matchesInRound + " mečeva";
        };
    }

    @Transactional(readOnly = true)
    public List<RoundView> americanRounds(Long tid) {
        List<TournamentMatch> ms = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "AMERICAN");
        Map<Integer, List<TournamentMatch>> byRound = new TreeMap<>();
        for (TournamentMatch m : ms) byRound.computeIfAbsent(m.getRoundNo(), k -> new ArrayList<>()).add(m);
        List<RoundView> out = new ArrayList<>();
        for (Map.Entry<Integer, List<TournamentMatch>> e : byRound.entrySet())
            out.add(new RoundView(e.getKey(), "Runda " + e.getKey(), e.getValue()));
        return out;
    }

    @Transactional(readOnly = true)
    public List<Standing> americanStandings(Long tid) {
        List<TournamentMatch> ms = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "AMERICAN");
        Map<Long, Standing> map = new LinkedHashMap<>();
        for (TournamentMatch m : ms) {
            register(map, m.getA1()); register(map, m.getA2());
            register(map, m.getB1()); register(map, m.getB2());
        }
        for (TournamentMatch m : ms) {
            if (m.getMatch() == null || !m.getMatch().isFinished()) continue;
            int sa = m.getMatch().getTeam1Total();
            int sb = m.getMatch().getTeam2Total();
            boolean aWon = m.getWinnerSide() == 1;
            acc(map, m.getA1(), sa, sb, aWon); acc(map, m.getA2(), sa, sb, aWon);
            acc(map, m.getB1(), sb, sa, !aWon); acc(map, m.getB2(), sb, sa, !aWon);
        }
        List<Standing> list = new ArrayList<>(map.values());
        list.sort(
                Comparator.comparingInt((Standing s) -> s.pf).reversed()
                        .thenComparing(Comparator.comparingInt((Standing s) -> s.wins).reversed())
        );
        return list;
    }

    private void register(Map<Long, Standing> map, Player p) {
        if (p != null) map.computeIfAbsent(p.getId(), k -> new Standing(p.getName()));
    }
    private void acc(Map<Long, Standing> map, Player p, int forPts, int againstPts, boolean won) {
        if (p == null) return;
        Standing s = map.get(p.getId());
        s.played++; s.pf += forPts; s.pa += againstPts; if (won) s.wins++;
    }

    @Transactional
    public void delete(Long tid) {
        tmRepo.deleteByTournamentId(tid);
        teamRepo.deleteByTournamentId(tid);
        tournamentRepo.deleteById(tid);
    }
}