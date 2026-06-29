package belatracker.controller;

import belatracker.model.Match;
import belatracker.model.Player;
import belatracker.service.MatchService;
import belatracker.service.PlayerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;
    private final PlayerService playerService;

    public MatchController(MatchService matchService, PlayerService playerService) {
        this.matchService = matchService;
        this.playerService = playerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("matches", matchService.getAllMatches());
        return "matches/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("match", new Match());
        model.addAttribute("players", playerService.getAllPlayers());
        return "matches/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Match match,
                       @RequestParam Long team1Player1Id,
                       @RequestParam Long team1Player2Id,
                       @RequestParam Long team2Player1Id,
                       @RequestParam Long team2Player2Id) {

        match.setTeam1Player1(playerService.getPlayerById(team1Player1Id));
        match.setTeam1Player2(playerService.getPlayerById(team1Player2Id));
        match.setTeam2Player1(playerService.getPlayerById(team2Player1Id));
        match.setTeam2Player2(playerService.getPlayerById(team2Player2Id));

        Match saved = matchService.saveMatch(match);
        return "redirect:/matches/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String board(@PathVariable Long id, Model model) {
        Match m = matchService.getMatchById(id);
        model.addAttribute("match", m);

        String[] order = {
            nameOf(m.getTeam1Player1()),
            nameOf(m.getTeam2Player1()),
            nameOf(m.getTeam1Player2()),
            nameOf(m.getTeam2Player2())
        };
        int start = 0;
        if (m.getDealer() != null) {
            for (int i = 0; i < order.length; i++) {
                if (order[i] != null && order[i].equalsIgnoreCase(m.getDealer())) { start = i; break; }
            }
        }
        int pos = (start + m.getRounds().size()) % 4;
        model.addAttribute("dealerName", order[pos]);
        return "matches/board";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return "redirect:/matches";
    }

    private String nameOf(Player p) {
        return p == null ? null : p.getName();
    }
}
