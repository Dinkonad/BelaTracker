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
    public String newForm(@PathVariable Long matchId,
                          @RequestParam(required = false) String back,
                          Model model) {
        model.addAttribute("round", new Round());
        model.addAttribute("matchId", matchId);
        model.addAttribute("back", back != null ? back : "/matches");
        return "matches/round-form";
    }

    @GetMapping("/edit/{roundId}")
    public String editForm(@PathVariable Long matchId, @PathVariable Long roundId,
                           @RequestParam(required = false) String back,
                           Model model) {
        model.addAttribute("round", matchService.getRound(roundId));
        model.addAttribute("matchId", matchId);
        model.addAttribute("back", back != null ? back : "/matches");
        return "matches/round-form";
    }

    @PostMapping("/save")
    public String save(@PathVariable Long matchId, @ModelAttribute Round round,
                       @RequestParam(required = false) String back) {
        if (round.getId() != null) {
            matchService.updateRound(matchId, round);
        } else {
            matchService.addRound(matchId, round);
        }
        String redirect = "/matches/" + matchId;
        if (back != null && !back.isEmpty()) redirect += "?back=" + back;
        return "redirect:" + redirect;
    }

    @GetMapping("/delete/{roundId}")
    public String delete(@PathVariable Long matchId, @PathVariable Long roundId,
                         @RequestParam(required = false) String back) {
        matchService.deleteRound(matchId, roundId);
        String redirect = "/matches/" + matchId;
        if (back != null && !back.isEmpty()) redirect += "?back=" + back;
        return "redirect:" + redirect;
    }
}