package belatracker.controller;

import belatracker.model.Match;
import belatracker.service.MatchService;
import belatracker.service.PlayerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("match", matchService.getMatchById(id));
        return "matches/board";
    }

    @PostMapping("/{id}/dealer")
    public String setDealer(@PathVariable Long id, @RequestParam String dealer) {
        matchService.setDealer(id, dealer);
        return "redirect:/matches/" + id;
    }

    @GetMapping("/{id}/finish")
    public String finish(@PathVariable Long id, RedirectAttributes ra) {
        try {
            matchService.finishMatch(id);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/matches/" + id;
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return "redirect:/matches";
    }
}
