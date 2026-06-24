package belatracker.controller;

import belatracker.model.Match;
import belatracker.service.MatchService;
import belatracker.service.PlayerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

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

        match.setDate(LocalDate.now());
        match.setTeam1Player1(playerService.getPlayerById(team1Player1Id));
        match.setTeam1Player2(playerService.getPlayerById(team1Player2Id));
        match.setTeam2Player1(playerService.getPlayerById(team2Player1Id));
        match.setTeam2Player2(playerService.getPlayerById(team2Player2Id));

        if (match.getTeam1Score() > match.getTeam2Score()) {
            match.setWinner(1);
        } else {
            match.setWinner(2);
        }

        matchService.saveMatch(match);
        return "redirect:/matches";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return "redirect:/matches";
    }
}