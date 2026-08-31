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
        if (n < 8 || n % 2 != 0)
            throw new IllegalArgumentException("Grupe + ždrijeb traže paran broj igrača, najmanje 8 (4 tima).");

        Collections.shuffle(players);

        Tournament t = new Tournament();
        t.setName((name == null || name.isBlank()) ? "Turnir" : name.trim());
        t.setFormat(format);
        t.setTargetScore(target);
        t.setDate(LocalDate.now());
        t.setStatus(TournamentStatus.IN_PROGRESS);
        t = tournamentRepo.save(t);

        List<TournamentTeam> teams = pairTeams(t, players);
        int g = Math.max(2, (int) Math.round(teams.size() / 4.0));
        buildGroupStage(t, teams, g);
        return t;
    }

    private List<TournamentTeam> pairTeams(Tournament t, List<Player> players) {
        List<TournamentTeam> teams = new ArrayList<>();
        for (int i = 0; i + 1 < players.size(); i += 2) {
            TournamentTeam tt = new TournamentTeam();
            tt.setTournament(t);
            tt.setPlayer1(players.get(i));
            tt.setPlayer2(players.get(i + 1));
            tt.setGroupNo(-1);
            teams.add(teamRepo.save(tt));
        }
        return teams;
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

    private void buildKnockout(Tournament t, List<TournamentTeam> seeds) {
        int n = seeds.size();
        int bracket = 1; while (bracket < n) bracket *= 2;
        int rounds = 0; { int p = 1; while (p < bracket) { p *= 2; rounds++; } }

        List<TournamentMatch> all = new ArrayList<>();
        for (int r = 1; r <= rounds; r++) {
            int cnt = bracket >> r;
            for (int s = 0; s < cnt; s++) {
                TournamentMatch m = new TournamentMatch();
                m.setTournament(t); m.setPhase("KO"); m.setRoundNo(r); m.setSlot(s);
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

    @Transactional
    public Match startOrContinue(Long tmId) {
        TournamentMatch tm = getTournamentMatch(tmId);
        if (tm.getMatch() != null) return tm.getMatch();
        if (tm.isBye() || !tm.isReady() || tm.getTeamA() == null || tm.getTeamB() == null)
            throw new IllegalStateException("Meč još nije spreman za igru.");

        Match match = new Match();
        match.setTeam1Player1(tm.getTeamA().getPlayer1());
        match.setTeam1Player2(tm.getTeamA().getPlayer2());
        match.setTeam2Player1(tm.getTeamB().getPlayer1());
        match.setTeam2Player2(tm.getTeamB().getPlayer2());
        match.setTargetScore(tm.getTournament().getTargetScore());
        match = matchService.saveMatch(match);

        tm.setMatch(match);
        tmRepo.save(tm);
        return match;
    }

    @Transactional
    public void syncFromMatch(Long matchId) {
        tmRepo.findByMatchId(matchId).ifPresent(tm -> {
            Match m = tm.getMatch();
            if (m.isFinished() && tm.getWinnerSide() == 0) {
                tm.setScoreA(m.getTeam1Total());
                tm.setScoreB(m.getTeam2Total());
                tm.setWinnerSide(m.getWinner());
                tmRepo.save(tm);
                sync(tm.getTournament().getId());
            } else if (!m.isFinished() && tm.getWinnerSide() != 0) {
                tm.setScoreA(null);
                tm.setScoreB(null);
                tm.setWinnerSide(0);
                tmRepo.save(tm);
            }
        });
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

        List<TournamentMatch> ko = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "KO");
        List<TournamentMatch> grp = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(tid, "GROUP");
        boolean allGroupDone = !grp.isEmpty() && grp.stream().allMatch(TournamentMatch::isDecided);
        if (ko.isEmpty() && allGroupDone) generateKnockoutFromGroups(t);

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
        buildKnockout(t, seeds);
    }

    private void checkFinished(Tournament t) {
        List<TournamentMatch> ko = tmRepo.findByTournamentIdAndPhaseOrderByRoundNoAscSlotAsc(t.getId(), "KO");
        if (ko.isEmpty()) return;
        int maxRound = ko.stream().mapToInt(TournamentMatch::getRoundNo).max().orElse(0);
        TournamentMatch fin = ko.stream().filter(m -> m.getRoundNo() == maxRound).findFirst().orElse(null);
        if (fin != null && fin.isDecided()) {
            TournamentTeam champ = fin.getWinnerSide() == 1 ? fin.getTeamA() : fin.getTeamB();
            if (champ != null) finish(t, champ.getLabel(), List.of(champ.getPlayer1(), champ.getPlayer2()));
        }
    }

    private void finish(Tournament t, String champion, List<Player> champions) {
        t.setStatus(TournamentStatus.FINISHED);
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

    @Transactional
    public void delete(Long tid) {
        tmRepo.deleteByTournamentId(tid);
        teamRepo.deleteByTournamentId(tid);
        tournamentRepo.deleteById(tid);
    }
}