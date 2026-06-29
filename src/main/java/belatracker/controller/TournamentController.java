package belatracker.controller;

import belatracker.model.TournamentFormat;
import belatracker.service.PlayerService;
import belatracker.service.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tournaments")
public class TournamentController {

    private final TournamentService service;
    private final PlayerService playerService;

    public TournamentController(TournamentService service, PlayerService playerService) {
        this.service = service;
        this.playerService = playerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tournaments", service.getAll());
        return "tournaments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("players", playerService.getAllPlayers());
        model.addAttribute("formats", TournamentFormat.values());
        return "tournaments/new";
    }

    @PostMapping("/save")
    public String save(@RequestParam String name,
                       @RequestParam TournamentFormat format,
                       @RequestParam int targetScore,
                       @RequestParam(name = "playerIds", required = false) List<Long> playerIds,
                       RedirectAttributes ra) {
        try {
            if (playerIds == null || playerIds.isEmpty())
                throw new IllegalArgumentException("Odaberi igrače za turnir.");
            var t = service.create(name, format, targetScore, playerIds);
            return "redirect:/tournaments/" + t.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tournaments/new";
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public String view(@PathVariable Long id, Model model) {
        try {
            service.sync(id);
            var t = service.get(id);
            model.addAttribute("tournament", t);

            switch (t.getFormat()) {
                case KNOCKOUT -> model.addAttribute("rounds", service.knockoutRounds(id));
                case LEAGUE -> {
                    model.addAttribute("standings", service.buildLeagueStandings(id));
                    model.addAttribute("leagueMatches", service.leagueMatches(id));
                }
                case GROUPS_KNOCKOUT -> {
                    model.addAttribute("groups", service.buildGroups(id));
                    model.addAttribute("rounds", service.knockoutRounds(id));
                }
                case AMERICAN -> {
                    model.addAttribute("americanRounds", service.americanRounds(id));
                    model.addAttribute("americanStandings", service.americanStandings(id));
                }
            }
            return "tournaments/view";
        } catch (Exception e) {
            model.addAttribute("error", "Greška pri učitavanju turnira: " + e.getMessage());
            return "tournaments/list";
        }
    }

    @GetMapping("/{id}/play/{tmId}")
    public String play(@PathVariable Long id, @PathVariable Long tmId, RedirectAttributes ra) {
        try {
            Long matchId = service.playMatch(tmId);
            return "redirect:/matches/" + matchId;
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tournaments/" + id;
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/tournaments";
    }
}
