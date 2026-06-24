package belatracker.controller;

import belatracker.model.Round;
import belatracker.service.MatchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/matches/{matchId}/rounds")
public class RoundController {

    private final MatchService matchService;

    public RoundController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long matchId, Model model) {
        model.addAttribute("round", new Round());
        model.addAttribute("matchId", matchId);
        return "matches/round-form";
    }

    @GetMapping("/edit/{roundId}")
    public String editForm(@PathVariable Long matchId, @PathVariable Long roundId, Model model) {
        model.addAttribute("round", matchService.getRound(roundId));
        model.addAttribute("matchId", matchId);
        return "matches/round-form";
    }

    @PostMapping("/save")
    public String save(@PathVariable Long matchId, @ModelAttribute Round round) {
        if (round.getId() != null) {
            matchService.updateRound(round);
        } else {
            matchService.addRound(matchId, round);
        }
        return "redirect:/matches/" + matchId;
    }

    @GetMapping("/delete/{roundId}")
    public String delete(@PathVariable Long matchId, @PathVariable Long roundId) {
        matchService.deleteRound(roundId);
        return "redirect:/matches/" + matchId;
    }
}
